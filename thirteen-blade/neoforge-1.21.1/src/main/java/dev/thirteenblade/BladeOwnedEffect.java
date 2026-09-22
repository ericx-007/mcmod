package dev.thirteenblade;

/** Ownership travels with the status instance through player saves; ordinary potions stay unowned. */
public interface BladeOwnedEffect {
    boolean thirteenblade$isOwned();
    void thirteenblade$setOwned(boolean owned);
}
