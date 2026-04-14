package com.oscaruiz.mycqrs.core.infrastructure.spring;

import com.oscaruiz.mycqrs.core.contracts.command.Command;
import com.oscaruiz.mycqrs.core.contracts.command.CommandInterceptor.CommandHandlerInvoker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionalCommandInterceptorTest {

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private TransactionStatus transactionStatus;

    private TransactionalCommandInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new TransactionalCommandInterceptor(transactionManager);
        when(transactionManager.getTransaction(any(TransactionDefinition.class)))
            .thenReturn(transactionStatus);
    }

    @Test
    void commits_after_handler_succeeds() {
        FakeCommand command = new FakeCommand();
        CommandHandlerInvoker next = mock(CommandHandlerInvoker.class);

        interceptor.intercept(command, next);

        InOrder inOrder = inOrder(transactionManager, next);
        inOrder.verify(transactionManager).getTransaction(any(TransactionDefinition.class));
        inOrder.verify(next).invoke(command);
        inOrder.verify(transactionManager).commit(transactionStatus);
        verify(transactionManager, never()).rollback(any());
    }

    @Test
    void rolls_back_and_propagates_when_handler_throws_runtime_exception() {
        FakeCommand command = new FakeCommand();
        CommandHandlerInvoker next = mock(CommandHandlerInvoker.class);
        RuntimeException boom = new IllegalStateException("boom");
        doThrow(boom).when(next).invoke(command);

        assertThatThrownBy(() -> interceptor.intercept(command, next))
            .isSameAs(boom);

        InOrder inOrder = inOrder(transactionManager, next);
        inOrder.verify(transactionManager).getTransaction(any(TransactionDefinition.class));
        inOrder.verify(next).invoke(command);
        inOrder.verify(transactionManager).rollback(transactionStatus);
        verify(transactionManager, never()).commit(any());
    }

    @Test
    void rolls_back_and_propagates_when_handler_throws_error() {
        FakeCommand command = new FakeCommand();
        CommandHandlerInvoker next = mock(CommandHandlerInvoker.class);
        Error fatal = new AssertionError("fatal");
        doThrow(fatal).when(next).invoke(command);

        assertThatThrownBy(() -> interceptor.intercept(command, next))
            .isSameAs(fatal);

        InOrder inOrder = inOrder(transactionManager, next);
        inOrder.verify(transactionManager).getTransaction(any(TransactionDefinition.class));
        inOrder.verify(next).invoke(command);
        inOrder.verify(transactionManager).rollback(transactionStatus);
        verify(transactionManager, never()).commit(any());
    }

    @Test
    void does_not_rollback_when_commit_fails() {
        FakeCommand command = new FakeCommand();
        CommandHandlerInvoker next = mock(CommandHandlerInvoker.class);
        RuntimeException commitFailure = new IllegalStateException("commit failed");
        doThrow(commitFailure).when(transactionManager).commit(transactionStatus);

        assertThatThrownBy(() -> interceptor.intercept(command, next))
            .isSameAs(commitFailure);

        verify(next).invoke(command);
        verify(transactionManager).commit(transactionStatus);
        verify(transactionManager, never()).rollback(any());
    }

    record FakeCommand() implements Command {}
}
