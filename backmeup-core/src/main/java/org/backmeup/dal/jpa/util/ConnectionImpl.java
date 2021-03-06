package org.backmeup.dal.jpa.util;

import java.util.Stack;
import java.util.concurrent.Callable;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.FlushModeType;

import org.backmeup.dal.Connection;
import org.backmeup.dal.ConnectionTemplate;
import org.backmeup.dal.DataAccessLayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * The Connection class makes the JPA transaction handling
 * easier for the BusinessLogicImpl class.
 * 
 * @author fschoeppl
 */
@ApplicationScoped
public class ConnectionImpl implements Connection {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionImpl.class);

    private EntityManagerFactory emFactory;
    private final ThreadLocal<EntityManager> threadLocalEntityManager;
    private final ThreadLocal<Stack<Boolean>> joinedTransactions;
    private DataAccessLayer dal;

    public ConnectionImpl() {
        this.threadLocalEntityManager = new ThreadLocal<>();
        joinedTransactions = new ThreadLocal<>(); 
    }

    @Inject
    public void setEntityManagerFactory(EntityManagerFactory emFactory) {
        this.emFactory = emFactory;
    }

    @Inject
    public void setDataAccessLayer(DataAccessLayer dal) {
        this.dal = dal;
    }

    private EntityManager getOrCreateEntityManager() {
        EntityManager em = getEntityManager();

        if (em == null) {
            em = emFactory.createEntityManager();
            threadLocalEntityManager.set(em);
            dal.setConnection(em);
        }

        return em;
    }

    @Override
    public void begin() {
        EntityManager em = getOrCreateEntityManager(); 

        if (em.getTransaction().isActive()) {
            LOGGER.debug("Warning: Transaction already active! Rolling back");
            em.getTransaction().rollback();
        }

        if (!em.getTransaction().isActive()) {
            em.setFlushMode(FlushModeType.COMMIT);
            em.getTransaction().begin();
        }
    }

    @Override
    public void rollback() {
        EntityManager em = getEntityManager();

        if (em == null) {
            return;
        }

        Stack<Boolean> transactionStack = joinedTransactions.get();
        if (transactionStack == null || transactionStack.isEmpty()) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            resetEntityManager();
        } else {
            transactionStack.pop();
        }
    }

    public EntityManager getEntityManager() {
        return threadLocalEntityManager.get();
    }

    @Override
    public void commit() {
        EntityManager em = getEntityManager();

        if (em == null) {
            LOGGER.debug("Has already been committed/rolled back!");
            return;
        }
        if (em.getTransaction().isActive()) {
            em.getTransaction().commit();
        }
        resetEntityManager();
    }

    private void resetEntityManager() {
        EntityManager em = getEntityManager();
        if (em != null) {
            em.close();
        }
        threadLocalEntityManager.set(null);
        dal.setConnection(null);
    }

    @Override
    public void beginOrJoin() {
        EntityManager em = getOrCreateEntityManager();

        if (!em.getTransaction().isActive()) {
            em.setFlushMode(FlushModeType.COMMIT);
            em.getTransaction().begin();
        } else {
            Stack<Boolean> transactionStack = joinedTransactions.get();
            if (transactionStack == null) { 
                transactionStack = new Stack<>();
            }
            transactionStack.push(true);
            joinedTransactions.set(transactionStack);
        }
    }

    @Override
    public <T> T txNew(Callable<T> getter) {
        return new ConnectionTemplate(this).insideNewTransaction(getter);
    }

    @Override
    public void txNew(Runnable call) {
        new ConnectionTemplate(this).insideNewTransaction(call);
    }

    @Override
    public <T> T txNewReadOnly(Callable<T> getter) {
        return new ConnectionTemplate(this).insideNewTransactionRolledBack(getter);
    }

    @Override
    public void txNewReadOnly(Runnable call) {
        new ConnectionTemplate(this).insideNewTransactionRolledBack(call);
    }

    @Override
    public void txJoin(Runnable call) {
        new ConnectionTemplate(this).insideJoinedTransaction(call);
    }

    @Override
    public void txJoinReadOnly(Runnable call) {
        new ConnectionTemplate(this).insideJoinedTransactionRolledBack(call);
    }

    @Override
    public <T> T txJoinReadOnly(Callable<T> getter) {
        return new ConnectionTemplate(this).insideJoinedTransactionRolledBack(getter);
    }
}
