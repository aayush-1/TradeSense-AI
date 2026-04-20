package ai.tradesense.portfolio;

import ai.tradesense.domain.Holding;
import ai.tradesense.domain.Position;

import java.util.List;

/**
 * Pluggable source of portfolio holdings and positions (e.g. mock, broker API).
 */
public interface PortfolioProvider {

    List<Holding> getHoldings();

    List<Position> getPositions();
}
