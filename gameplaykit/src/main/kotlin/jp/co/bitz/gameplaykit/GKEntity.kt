package jp.co.bitz.gameplaykit

import kotlin.reflect.KClass
import kotlin.time.Duration

public open class GKEntity {
    private val componentsByClass = LinkedHashMap<KClass<out GKComponent>, GKComponent>()

    public open val components: List<GKComponent>
        get() = componentsByClass.values.toList()

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

    // Exact-class lookup, matching GameplayKit: querying a supertype does not find a subclass's component.
    public open fun <T : GKComponent> componentForClass(componentClass: KClass<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return componentsByClass[componentClass] as? T
    }

    public inline fun <reified T : GKComponent> component(): T? = componentForClass(T::class)

    public open fun removeComponent(componentClass: KClass<out GKComponent>) {
        val existing = componentsByClass.remove(componentClass) ?: return
        existing.entity = null
        existing.willRemoveFromEntity()
    }

    public inline fun <reified T : GKComponent> removeComponent() {
        removeComponent(T::class)
    }

    public open fun update(deltaTime: Duration) {
        componentsByClass.values.toList().forEach { it.update(deltaTime) }
    }
}
