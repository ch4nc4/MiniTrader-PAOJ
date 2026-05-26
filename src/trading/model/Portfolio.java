package trading.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Portfolio implements Printable {
    private final User user;
    private final Map<String, Double> balances;
    private final List<Position> positions;

    public Portfolio(User user) {
        this.user = user;
        this.balances = new HashMap<>();
        this.positions = new ArrayList<>();
    }

    public User getUser() {
        return user;
    }

    public Map<String, Double> getBalances() {
        return balances;
    }

    public double getBalance(String asset) {
        return balances.getOrDefault(asset, 0.0);
    }

    public void deposit(String asset, double amount) {
        balances.merge(asset, amount, Double::sum);
    }

    public void withdraw(String asset, double amount) {
        double current = getBalance(asset);
        if (current < amount) {
            throw new trading.exception.TradingException(
                    String.format("Insufficient %s balance: have %.2f, need %.2f", asset, current, amount));
        }
        balances.put(asset, current - amount);
    }

    public List<Position> getPositions() {
        return positions;
    }

    public List<Position> getOpenPositions() {
        List<Position> open = new ArrayList<>();
        for (Position p : positions) {
            if (p.isOpen()) {
                open.add(p);
            }
        }
        return open;
    }

    public void addPosition(Position position) {
        positions.add(position);
    }

    @Override
    public String toPrettyString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("=== Portfolio of %s ===\n", user.getUsername()));
        sb.append("Balances:\n");
        if (balances.isEmpty()) {
            sb.append("  (none)\n");
        } else {
            for (Map.Entry<String, Double> entry : balances.entrySet()) {
                sb.append(String.format("  %s: %.4f\n", entry.getKey(), entry.getValue()));
            }
        }
        List<Position> openPositions = getOpenPositions();
        sb.append(String.format("Open Positions (%d):\n", openPositions.size()));
        if (openPositions.isEmpty()) {
            sb.append("  (none)\n");
        } else {
            for (Position p : openPositions) {
                sb.append("  ").append(p.toPrettyString()).append("\n");
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return toPrettyString();
    }
}
