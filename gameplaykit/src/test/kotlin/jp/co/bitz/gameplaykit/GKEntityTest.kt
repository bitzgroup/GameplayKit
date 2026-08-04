package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class GKEntityTest {
    @Test
    fun `addComponent makes the component retrievable by its exact type`() {
        val entity = GKEntity()
        val movement = MovementComponent()

        entity.addComponent(movement)

        assertSame(movement, entity.component<MovementComponent>())
    }

    @Test
    fun `component lookup does not match by supertype, mirroring GameplayKit`() {
        val entity = GKEntity()
        entity.addComponent(SubComponent())

        assertNull(entity.component<BaseComponent>())
        assertTrue(entity.component<SubComponent>() != null)
    }

    @Test
    fun `adding a second component of the same class replaces the first`() {
        val entity = GKEntity()
        val first = MovementComponent()
        val second = MovementComponent()

        entity.addComponent(first)
        entity.addComponent(second)

        assertNull(first.entity)
        assertSame(entity, second.entity)
        assertSame(second, entity.component<MovementComponent>())
        assertEquals(1, entity.components.size)
    }

    @Test
    fun `removeComponent detaches the component from the entity`() {
        val entity = GKEntity()
        val movement = MovementComponent()
        entity.addComponent(movement)

        entity.removeComponent<MovementComponent>()

        assertNull(movement.entity)
        assertNull(entity.component<MovementComponent>())
    }

    @Test
    fun `update delegates to every component`() {
        val entity = GKEntity()
        val movement = MovementComponent()
        val health = HealthComponent()
        entity.addComponent(movement)
        entity.addComponent(health)

        entity.update(1.seconds)

        assertEquals(1, movement.updateCount)
        assertEquals(1, health.updateCount)
    }
}
