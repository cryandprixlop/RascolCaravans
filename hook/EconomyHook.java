package ru.raskol.caravans.model;

import java.util.UUID;

public final class Caravan {
    private final String id;
    private final UUID owner;
    private final int tier;
    private final String route;
    private final double amount;
    private final int guards;
    private final long departMillis;
    private final long arriveMillis;

    public Caravan(String id, UUID owner, int tier, String route, double amount, int guards, long departMillis, long arriveMillis) {
        this.id = id; this.owner = owner; this.tier = tier; this.route = route;
        this.amount = amount; this.guards = guards; this.departMillis = departMillis; this.arriveMillis = arriveMillis;
    }

    public String getId() { return id; }
    public UUID getOwner() { return owner; }
    public int getTier() { return tier; }
    public String getRoute() { return route; }
    public double getAmount() { return amount; }
    public int getGuards() { return guards; }
    public long getDepartMillis() { return departMillis; }
    public long getArriveMillis() { return arriveMillis; }
    public boolean isReady(long now) { return now >= arriveMillis; }
}
