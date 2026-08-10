package jp.co.bitz.gameplaykit

import kotlin.reflect.KClass
import kotlin.time.Duration

/**
 * A homogeneous, ordered collection of [GKComponent]s of type [T], mirroring GameplayKit's
 * `GKComponentSystem`. Useful for updating every component of a given kind across all entities in
 * one pass (e.g. every physics component, then every render component), rather than updating each
 * entity's components together via [GKEntity.update].
 */
public open class GKComponentSystem<T : GKComponent>(
    public val componentClass: KClass<T>,
) : Iterable<T> {
    private val mutableComponents = mutableListOf<T>()

    /** The components currently in this system, in the order they were added. */
    public open val components: List<T>
        get() = mutableComponents.toList()

    /** Adds [component] to this system, if not already present. */
    public open fun addComponent(component: T) {
        if (component !in mutableComponents) {
            mutableComponents.add(component)
        }
    }

    /** Adds `foundIn`'s component of [componentClass] to this system, if it has one. */
    public open fun addComponent(foundIn: GKEntity) {
        val component = foundIn.componentForClass(componentClass) ?: return
        addComponent(component)
    }

    /** Removes [component] from this system. */
    public open fun removeComponent(component: T) {
        mutableComponents.remove(component)
    }

    /** Removes `foundIn`'s component of [componentClass] from this system, if it has one. */
    public open fun removeComponent(foundIn: GKEntity) {
        val component = foundIn.componentForClass(componentClass) ?: return
        removeComponent(component)
    }

    /** Calls [GKComponent.update] on every component in this system, in order. */
    public open fun update(deltaTime: Duration) {
        mutableComponents.toList().forEach { it.update(deltaTime) }
    }

    /** Returns the component at [index]. */
    public open operator fun get(index: Int): T = mutableComponents[index]

    override fun iterator(): Iterator<T> = mutableComponents.iterator()
}

/** Reified convenience for constructing a [GKComponentSystem] without passing `T::class` explicitly. */
@Suppress("ktlint:standard:function-naming", "FunctionName")
public inline fun <reified T : GKComponent> GKComponentSystem(): GKComponentSystem<T> = GKComponentSystem(T::class)
