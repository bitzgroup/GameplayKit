package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds

class GKComponentTest {
    @Test
    fun `entity is null until added to an entity`() {
        val component = RecordingComponent()
        assertNull(component.entity)
    }

    @Test
    fun `entity reflects the owning entity after being added`() {
        val entity = GKEntity()
        val component = RecordingComponent()

        entity.addComponent(component)

        assertEquals(entity, component.entity)
    }

    @Test
    fun `update increments the recorded count and delta`() {
        val component = RecordingComponent()

        component.update(0.5.seconds)

        assertEquals(1, component.updateCount)
        assertEquals(0.5.seconds, component.lastDeltaTime)
    }
}
