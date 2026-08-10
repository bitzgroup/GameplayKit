package jp.co.bitz.gameplaykit

import kotlin.reflect.KClass
import kotlin.time.Duration

/**
 * A container for [GKComponent] instances that together define an object in your game, mirroring
 * GameplayKit's `GKEntity`. An entity holds at most one component per concrete component class;
 * adding a second component of the same class replaces the first.
 */
public open class GKEntity {
    private val componentsByClass = LinkedHashMap<KClass<out GKComponent>, GKComponent>()

    /** The components currently attached to this entity, in the order they were added. */
    public open val components: List<GKComponent>
        get() = componentsByClass.values.toList()

    /**
     * Attaches [component] to this entity, replacing any existing component of the same concrete
     * class. The replaced component (if any) has [GKComponent.entity] cleared and
     * [GKComponent.willRemoveFromEntity] called; [component] has [GKComponent.didAddToEntity]
     * called once attached.
     */
    public open fun addComponent(component: GKComponent) {
        val componentClass = component::class
        componentsByClass.remove(componentClass)?.let { existing ->
            existing.entity = null
            existing.willRemoveFromEntity()
        }
        component.entity = this
        componentsByClass[componentClass] = component
        component.didAddToEntity()
    }

    /**
     * Returns the attached component whose concrete class is exactly [componentClass], or `null`
     * if none is attached. Exact-class lookup, matching GameplayKit: querying a supertype does
     * not find a subclass's component.
     */
    public open fun <T : GKComponent> componentForClass(componentClass: KClass<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return componentsByClass[componentClass] as? T
    }

    /** Reified convenience for [componentForClass]. */
    public inline fun <reified T : GKComponent> component(): T? = componentForClass(T::class)

    /**
     * Detaches the component whose concrete class is [componentClass], if attached, clearing its
     * [GKComponent.entity] and calling [GKComponent.willRemoveFromEntity]. Does nothing if no such
     * component is attached.
     */
    public open fun removeComponent(componentClass: KClass<out GKComponent>) {
        val existing = componentsByClass.remove(componentClass) ?: return
        existing.entity = null
        existing.willRemoveFromEntity()
    }

    /** Reified convenience for [removeComponent]. */
    public inline fun <reified T : GKComponent> removeComponent() {
        removeComponent(T::class)
    }

    /** Calls [GKComponent.update] on every attached component, in attachment order. */
    public open fun update(deltaTime: Duration) {
        componentsByClass.values.toList().forEach { it.update(deltaTime) }
    }
}
