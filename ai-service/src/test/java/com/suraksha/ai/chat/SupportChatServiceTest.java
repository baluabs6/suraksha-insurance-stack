package com.suraksha.ai.chat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SupportChatServiceTest {

    @Test
    void memoryKeyIsNamespacedByUser() {
        assertNotEquals(SupportChatService.memoryKey("user-a", "c1"), SupportChatService.memoryKey("user-b", "c1"));
        assertEquals("user-a:c1", SupportChatService.memoryKey("user-a", "c1"));
    }

    @Test
    void missingConversationIdFallsBackToDefault() {
        assertEquals("user-a:default", SupportChatService.memoryKey("user-a", null));
        assertEquals("user-a:default", SupportChatService.memoryKey("user-a", "  "));
    }

    @Test
    void rejectsConversationIdsThatCouldEscapeTheNamespace() {
        assertThrows(IllegalArgumentException.class, () -> SupportChatService.memoryKey("user-a", "x:user-b:c1"));
        assertFalse(SupportChatService.isValidConversationId("../etc"));
        assertTrue(SupportChatService.isValidConversationId("3f2a-91_bc"));
    }
}
