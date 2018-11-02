package com.bank.dao;

import com.bank.entity.BankAccount;
import com.bank.entity.Card;
import com.bank.entity.Customer;
import com.bank.pool.Pool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;

public class CardDAOImpl extends AbstractDAO implements CardDAO {

    private Logger log = LoggerFactory.getLogger(BankAccountDAOImpl.class);

    private static class Holder {
        static final CardDAO CARD_DAO = new CardDAOImpl();
    }

    public static CardDAO getInstance() {
        return Holder.CARD_DAO;
    }

    @Override
    public long save(Card card) {

        PreparedStatement statement = null;
        ResultSet resultSet = null;

        long customerId = CustomerDAOImpl.getInstance().save(card.getCustomer());
        long cardId = 0;

        try (Connection connection = Pool.getConnection()) {

            statement = connection
                    .prepareStatement("INSERT INTO cards (number, customer_id) VALUES (?,?)", Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, card.getNumber().replaceAll("\\D", ""));
            statement.setLong(2, customerId);
            statement.execute();
            connection.commit();
            resultSet = statement.getGeneratedKeys();
            resultSet.next();
            cardId = resultSet.getLong(1);

        } catch (SQLException e) {

            if (e.getMessage().contains("duplicate key value")) {

                cardId = CardDAOImpl.getInstance().getCardId(card);

            } else {

                log.error(e.getMessage(), e);
            }
        } finally {
            Helper.closeStatementResultSet(statement, resultSet);
        }

        return cardId;
    }

    @Override
    public Collection<Card> getAll() {

        PreparedStatement statement = null;
        ResultSet resultSet = null;
        Collection<Card> cards = new ArrayList<>();

        final String SELECT_QUERY = "SELECT cust.name, cust.surname, cust.phone, card.number as card, ba.account, ba.deposit, ba.credit".concat(
                " FROM customers cust JOIN cards card ON cust.id = card.customer_id").concat(
                " JOIN bank_accounts ba ON card.id = ba.card_id");

        try (Connection connection = Pool.getConnection()) {

            statement = connection.prepareStatement(SELECT_QUERY);
            resultSet = statement.executeQuery();
            connection.commit();

            while (resultSet.next()) {

                Customer customer = new Customer();
                BankAccount bankAccount = new BankAccount();
                Card card = new Card();

                customer.setName(resultSet.getString(1));
                customer.setSurname(resultSet.getString(2));
                customer.setPhone(resultSet.getString(3));
                card.setNumber(resultSet.getString(4));
                bankAccount.setAccount(resultSet.getString(5));
                bankAccount.setDeposit(resultSet.getBigDecimal(6));
                bankAccount.setCredit(resultSet.getBigDecimal(7));
                card.setCustomer(customer);
                card.setBankAccount(bankAccount);
                cards.add(card);

            }

        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        } finally {
            Helper.closeStatementResultSet(statement, resultSet);
        }

        return cards;
    }

    @Override
    public void deleteCard(Card card) {

    }

    @Override
    public Collection<Card> getCards(Customer customer) {

        PreparedStatement statement = null;
        ResultSet resultSet = null;
        Collection<Card> cards = new ArrayList<>();
        final String EXECUTE_QUERY = "SELECT cards.number, ba.account, ba.deposit, ba.credit FROM cards".concat(
                " JOIN bank_accounts ba ON cards.bank_account_id = ba.id").concat(
                " WHERE customer_id = (SELECT id FROM customers WHERE phone = ?)");


        try (Connection connection = Pool.getConnection()) {

            statement = connection
                    .prepareStatement(EXECUTE_QUERY);
            resultSet = statement.executeQuery();
            connection.commit();

            while (resultSet.next()) {

                Card card = new Card();
                BankAccount bankAccount = new BankAccount();
                card.setNumber(resultSet.getString(1));
                bankAccount.setAccount(resultSet.getString(2));
                bankAccount.setDeposit(resultSet.getBigDecimal(3));
                bankAccount.setCredit(resultSet.getBigDecimal(4));
                card.setBankAccount(bankAccount);
                cards.add(card);

            }

        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        } finally {
            Helper.closeStatementResultSet(statement, resultSet);
        }

        return cards;
    }

    @Override
    public long getCardId(Card card) {

        PreparedStatement statement = null;
        ResultSet resultSet = null;
        long cardId = 0;

        try (Connection connection = Pool.getConnection()) {

            statement = connection.prepareStatement("SELECT id FROM cards WHERE number = ?");
            statement.setString(1, card.getNumber());
            resultSet = statement.executeQuery();
            connection.commit();
            resultSet.next();
            cardId = resultSet.getLong(1);

        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        } finally {
            Helper.closeStatementResultSet(statement, resultSet);
        }

        return cardId;
    }
}
