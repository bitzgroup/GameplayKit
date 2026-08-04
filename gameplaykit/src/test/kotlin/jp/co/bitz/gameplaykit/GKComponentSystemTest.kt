package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.time.Duration.Companion.seconds

class GKComponentSystemTest {
    @Test
    fun `reified constructor stores the requested component class`() {
        val system = GKComponentSystem<MovementComponent>()

        assertEquals(MovementComponent::class, system.componentClass)
    }

    @Test
    fun `addComponent appends and get returns by index in insertion order`() {
        val system = GKComponentSystem<MovementComponent>()
        val first = MovementComponent()
        val second = MovementComponent()

        system.addComponent(first)
        system.addComponent(second)

        assertSame(first, system[0])
        assertSame(second, system[1])
    }

    @Test
    fun `addComponent is idempotent for the same instance`() {
        val system = GKComponentSystem<MovementComponent>()
        val component = MovementComponent()

        system.addComponent(component)
        system.addComponent(component)

        assertEquals(1, system.components.size)
    }

    @Test
    fun `addComponent foundIn adds the entity's matching component`() {
        val system = GKComponentSystem<MovementComponent>()
        val entity = GKEntity()
        val movement = MovementComponent()
        entity.addComponent(movement)

        system.addComponent(entity)

        assertSame(movement, system[0])
    }

    @Test
    fun `addComponent foundIn is a no-op when the entity has no matching component`() {
        val system = GKComponentSystem<MovementComponent>()
        val entity = GKEntity()

        system.addComponent(entity)

        assertEquals(0, system.components.size)
    }

    @Test
    fun `removeComponent foundIn removes the entity's matching component`() {
        val system = GKComponentSystem<MovementComponent>()
        val entity = GKEntity()
        val movement = MovementComponent()
        entity.addComponent(movement)
        system.addComponent(entity)

        system.removeComponent(entity)

        assertEquals(0, system.components.size)
    }

    @Test
    fun `update delegates to every managed component`() {
        val system = GKComponentSystem<MovementComponent>()
        val first = MovementComponent()
        val second = MovementComponent()
        system.addComponent(first)
        system.addComponent(second)

        system.update(1.seconds)

        assertEquals(1, first.updateCount)
        assertEquals(1, second.updateCount)
    }

    @Test
    fun `system is iterable`() {
        val system = GKComponentSystem<MovementComponent>()
        val component = MovementComponent()
        system.addComponent(component)

        val collected = system.toList()

        assertEquals(listOf(component), collected)
    }

    @Test
    fun `KClass constructor is equivalent to the reified convenience constructor`() {
        val system = GKComponentSystem(MovementComponent::class)

        assertNull(system.components.firstOrNull())
    }
}
