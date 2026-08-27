package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.formver.core.embeddings.properties.BackingFieldGetter
import org.jetbrains.kotlin.formver.core.embeddings.properties.FieldEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.properties.PropertyEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.ClassTypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.PretypeEmbedding
import org.jetbrains.kotlin.formver.core.names.NameMatcher
import org.jetbrains.kotlin.formver.core.names.NameScope
import org.jetbrains.kotlin.formver.core.names.ScopedName
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.name.Name

/**
 * Name used to look up properties.
 */
data class PropertyKotlinName(val scope: NameScope, val name: Name)
data class ClassPropertyPair(val className : ScopedName, val propertyName : PropertyKotlinName)

class TypeResolver {
    /**
     * Collection of all ClassTypeEmbeddings and InterfaceTypeEmbeddings
     */
    private val classEmbedding = mutableMapOf<SymbolicName, ClassTypeEmbedding>()
    private val interfaceEmbedding = mutableMapOf<SymbolicName, ClassTypeEmbedding>()

    /**
     * Supertype relation. Key is a subtype, value is a set of supertypes.
     * The transitive closure is not included.
     */
    private val superTypes = mutableMapOf<SymbolicName, MutableSet<SymbolicName>>()

    /**
     * All the properties. Key is the pair of the class name and the field name.
     */
    private val properties = mutableMapOf<ClassPropertyPair, PropertyEmbedding>()

    /**
     * Names of classes marked `@Manual`.
     */
    private val manualClassNames = mutableSetOf<SymbolicName>()

    /**
     * Register a class or interface type embedding.
     * This is needed to know which classes were already registered.
     */
    fun register(typeEmbedding: ClassTypeEmbedding, isInterface: Boolean) = when (isInterface) {
        true -> interfaceEmbedding.putIfAbsent(typeEmbedding.name, typeEmbedding)
        false -> classEmbedding.putIfAbsent(typeEmbedding.name, typeEmbedding)
    }

    fun markManual(name: SymbolicName) {
        manualClassNames.add(name)
    }

    val ClassTypeEmbedding.isManual: Boolean
        get() = name in manualClassNames

    private fun toBackingField(property: PropertyEmbedding): FieldEmbedding? =
        (property.getter as? BackingFieldGetter)?.field


    fun classTypeEmbeddings() = (classEmbedding.values.toList() + interfaceEmbedding.values.toList()).distinctBy { it.name }

    fun getEmbeddingOrExecute(name: ScopedName, action: () -> ClassTypeEmbedding) = classEmbedding[name] ?: action()

    fun lookupClassTypeEmbedding(name: SymbolicName) = classEmbedding[name] ?: interfaceEmbedding[name]

    /**
     * Extends the subtype relation with [subtype] <: [supertype]
     */
    fun addSubtypeRelation(subtype: SymbolicName, supertype: SymbolicName) = superTypes.getOrPut(subtype) {
        mutableSetOf()
    }.add(supertype)


    /**
     * Returns the set of super types of a given class or interface.
     * These are only the direct super types, transitive closure is not included.
     */
    fun lookupSuperTypes(name: SymbolicName) =
        superTypes.getOrDefault(name, emptySet()).mapNotNull { lookupClassTypeEmbedding(it) }

    /**
     * Get or Put a property to the class.
     */
    fun getOrPutProperty(name: ClassPropertyPair, create: () -> PropertyEmbedding) = properties.getOrPut(name, create)


    /**
     * Returns all the fields belonging directly to a class.
     */
    fun lookupClassBackingFields(name: SymbolicName) =
        properties.filterKeys { it.className == name }.values.mapNotNull { toBackingField(it) }

    fun lookupBackingField(name: ClassPropertyPair): FieldEmbedding? = properties[name]?.let { toBackingField(it) }

    fun lookupClassDefaultBehavingProperties(name: SymbolicName): List<PropertyEmbedding> =
        lookupClassProperties(name).filter { it.hasDefaultBehaviour }

    fun lookupClassProperties(name: SymbolicName): List<PropertyEmbedding> =
        properties.filterKeys { it.className == name }.values.toList()

    fun lookupDefaultBehavingProperties(name: ClassPropertyPair): PropertyEmbedding? =
        properties[name]?.takeIf { it.hasDefaultBehaviour }


    fun backingFields(): List<FieldEmbedding> = properties.values.mapNotNull { toBackingField(it) }


    /**
     * Collects all the fields belonging to a class and its super types.
     * They are unique with regard to their name.
     */
    private fun collectFields(className: SymbolicName): List<FieldEmbedding> {
        return lookupSuperTypes(className).fold(lookupClassBackingFields(className)) { acc, type ->
            acc + collectFields(type.name)
        }.distinctBy { it.name }
    }

    fun <R> flatMapUniqueFields(
        className: SymbolicName,
        action: (FieldEmbedding) -> List<R>
    ): List<R> = collectFields(className).flatMap { action(it) }


    /**
     * Returns the sequence of class types that are between the [typeEmbedding] and the [field].
     */
    fun hierarchyPathTo(
        typeEmbedding: PretypeEmbedding,
        field: FieldEmbedding
    ): Sequence<ClassTypeEmbedding> = sequence {
        val classType = (typeEmbedding as? ClassTypeEmbedding) ?: return@sequence
        val className = field.containingClass?.name
        require(className != null) { "Cannot find hierarchy unfold path of a field with no class information" }
        if (className == typeEmbedding.name) {
            yield(classType)
        } else {
            val sup = lookupSuperTypes(classType.name).firstOrNull { !interfaceEmbedding.containsKey(it.name) }
                ?: throw IllegalArgumentException("Reached top of the hierarchy without finding the field")

            yield(classType)
            yieldAll(hierarchyPathTo(sup, field))
        }
    }

    /**
     * Returns true if the [pretypeEmbedding] is a subtype of Collection with [name]
     */
    fun isInheritorOfCollectionTypeNamed(pretypeEmbedding: PretypeEmbedding, name: String): Boolean {
        val classEmbedding = pretypeEmbedding as? ClassTypeEmbedding ?: return false
        return classEmbedding.isCollectionTypeNamed(name) || lookupSuperTypes(classEmbedding.name).any {
            isInheritorOfCollectionTypeNamed(it, name)
        }
    }

    fun isCollectionInheritor(pretype: PretypeEmbedding) = isInheritorOfCollectionTypeNamed(pretype, "Collection")

    fun PretypeEmbedding.isCollectionTypeNamed(name: String): Boolean {
        val classEmbedding = this as? ClassTypeEmbedding ?: return false
        NameMatcher.matchGlobalScope(classEmbedding.name) {
            ifInCollectionsPkg {
                ifClassName(name) {
                    return true
                }
            }
            return false
        }
    }

}
