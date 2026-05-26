package trading.service;

import trading.dao.BalanceDao;
import trading.dao.BalanceDaoImpl;
import trading.dao.UserDao;
import trading.dao.UserDaoImpl;
import trading.exception.TradingException;
import trading.model.Portfolio;
import trading.model.User;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UserService {
    private static UserService instance;

    private final UserDao userDao;
    private final BalanceDao balanceDao;
    private final Map<Integer, Portfolio> portfolios = new HashMap<>();

    private UserService(UserDao userDao, BalanceDao balanceDao) {
        this.userDao = userDao;
        this.balanceDao = balanceDao;
    }

    public static synchronized UserService getInstance(UserDao userDao, BalanceDao balanceDao) {
        if (instance == null) {
            instance = new UserService(userDao, balanceDao);
        }
        return instance;
    }

    public static synchronized UserService getInstance(UserDao userDao) {
        if (instance == null) {
            instance = new UserService(userDao, new BalanceDaoImpl());
        }
        return instance;
    }

    public static synchronized UserService getInstance() {
        if (instance == null) {
            instance = new UserService(new UserDaoImpl(), new BalanceDaoImpl());
        }
        return instance;
    }

    public static synchronized void resetInstance() {
        instance = null;
    }

    public UserDao getUserDao() {
        return userDao;
    }

    public User registerUser(String username, String email) {
        if (username == null || username.isBlank()) {
            throw new TradingException("Username cannot be empty.");
        }
        if (email == null || !email.contains("@")) {
            throw new TradingException("Invalid email address.");
        }
        if (userDao.findByUsername(username).isPresent()) {
            throw new TradingException("Username '" + username + "' is already taken.");
        }
        User user = userDao.create(username, email);
        portfolios.put(user.getId(), new Portfolio(user));
        AuditService.getInstance().log("REGISTER_USER");
        return user;
    }

    public User getUserById(int id) {
        return userDao.findById(id)
                .orElseThrow(() -> new TradingException("User with id " + id + " not found."));
    }

    public User getUserByUsername(String username) {
        return userDao.findByUsername(username)
                .orElseThrow(() -> new TradingException("User '" + username + "' not found."));
    }

    public Set<User> getAllUsers() {
        return new HashSet<>(userDao.findAll());
    }

    public Portfolio getPortfolio(User user) {
        Portfolio portfolio = portfolios.get(user.getId());
        if (portfolio == null) {
            portfolio = new Portfolio(user);
            Map<String, Double> savedBalances = balanceDao.findByUserId(user.getId());
            for (Map.Entry<String, Double> entry : savedBalances.entrySet()) {
                portfolio.deposit(entry.getKey(), entry.getValue());
            }
            portfolios.put(user.getId(), portfolio);
        }
        return portfolio;
    }

    public void deposit(User user, String asset, double amount) {
        if (amount <= 0) {
            throw new TradingException("Deposit amount must be positive.");
        }
        Portfolio portfolio = getPortfolio(user);
        portfolio.deposit(asset, amount);
        balanceDao.save(user.getId(), asset, portfolio.getBalance(asset));
        AuditService.getInstance().log("DEPOSIT_FUNDS");
    }

    public void withdraw(User user, String asset, double amount) {
        if (amount <= 0) {
            throw new TradingException("Withdrawal amount must be positive.");
        }
        Portfolio portfolio = getPortfolio(user);
        portfolio.withdraw(asset, amount);
        balanceDao.save(user.getId(), asset, portfolio.getBalance(asset));
        AuditService.getInstance().log("WITHDRAW_FUNDS");
    }

    public void persistBalances(User user) {
        Portfolio portfolio = portfolios.get(user.getId());
        if (portfolio != null) {
            for (Map.Entry<String, Double> entry : portfolio.getBalances().entrySet()) {
                balanceDao.save(user.getId(), entry.getKey(), entry.getValue());
            }
        }
    }
}