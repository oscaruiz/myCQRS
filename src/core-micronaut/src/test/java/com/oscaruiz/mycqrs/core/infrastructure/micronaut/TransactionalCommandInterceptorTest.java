package com.oscaruiz.mycqrs.core.infrastructure.micronaut;

import com.oscaruiz.mycqrs.core.contracts.command.Command;
import com.oscaruiz.mycqrs.core.contracts.command.CommandInterceptor;
import io.micronaut.transaction.SynchronousTransactionManager;
import io.micronaut.transaction.TransactionCallback;
import io.micronaut.transaction.TransactionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionalCommandInterceptorTest {

    @Mock
    private SynchronousTransactionManager<Object> transactionManager;

    @Mock
    private CommandInterceptor.CommandHandlerInvoker next;

    private TransactionalCommandInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new TransactionalCommandInterceptor(transactionManager);
        // Make the mock actually invoke the callback it receives, mimicking a real tx
        // manager's happy-path behaviour without coupling the test to a real DataSource.
        when(transactionManager.executeWrite(any())).thenAnswer(inv -> {
            TransactionCallback<Object, Object> callback = inv.getArgument(0);
            return callback.call(mock(TransactionStatus.class));
        });
    }

    @Test
    void wrapsNextInsideExecuteWrite() {
        Command command = new FakeCommand();

        interceptor.intercept(command, next);

        verify(transactionManager).executeWrite(any());
        verify(next).invoke(command);
    }

    @Test
    void propagatesExceptionsFromNext() {
        Command command = new FakeCommand();
        RuntimeException boom = new RuntimeException("boom");
        org.mockito.Mockito.doThrow(boom).when(next).invoke(command);

        assertThatThrownBy(() -> interceptor.intercept(command, next))
                .isSameAs(boom);
    }

    private record FakeCommand(UUID commandId) implements Command {
        FakeCommand() {
            this(UUID.randomUUID());
        }
    }
}
