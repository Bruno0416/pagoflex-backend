package com.pagoflex.usuarios.core.cache;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisTransactionExecutorTest {
    @Mock
    private StringRedisTemplate redis;
    @Mock
    private RedisOperations<String, String> operations;

    @Test
    void commitExecutesChangesAtomically() {
        when(operations.exec()).thenReturn(List.of(true));
        RedisTransactionExecutor executor = new RedisTransactionExecutor(redis);
        assertThat(executor.commit(operations, () -> operations.delete("key"))).containsExactly(true);
        var order = inOrder(operations);
        order.verify(operations).multi();
        order.verify(operations).delete("key");
        order.verify(operations).exec();
    }

    @Test
    void discardsTransactionIfLocalOperationThrows() {
        RedisTransactionExecutor executor = new RedisTransactionExecutor(redis);
        assertThatThrownBy(() -> executor.commit(operations, () -> {
            throw new IllegalStateException("fallo");
        })).isInstanceOf(IllegalStateException.class);
        verify(operations).discard();
        verify(operations, never()).exec();
    }
}
