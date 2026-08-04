package jp.co.bitz.gameplaykit

import kotlin.reflect.KClass
import kotlin.time.Duration

public open class GKComponentSystem<T : GKComponent>(
    public val componentClass: KClass<T>,
) : Iterable<T> {
    private val mutableComponents = mutableListOf<T>()

    public open val components: List<T>
        get() = mutableComponents.toList()

    public open fun addComponent(component: T) {
        if (component !in mutableComponents) {
            mutableComponents.add(component)
        }
    }

    public open fun addComponent(foundIn: GKEntity) {
        val component = foundIn.componentForClass(componentClass) ?: return
        addComponent(component)
    }

    public open fun removeComponent(component: T) {
        mutableComponents.remove(component)
    }

    public open fun removeComponent(foundIn: GKEntity) {
        val component = foundIn.componentForClass(componentClass) ?: return
        removeComponent(component)
    }

    public open fun update(deltaTime: Duration) {
        mutableComponents.toList().forEach { it.update(deltaTime) }
    }

    public open operator fun get(index: Int): T = mutableComponents[index]

    override fun iterator(): Iterator<T> = mutableComponents.iterator()
}

@Suppress("ktlint:standard:function-naming", "FunctionName")
public inline fun <reified T : GKComponent> GKComponentSystem(): GKComponentSystem<T> = GKComponentSystem(T::class)
