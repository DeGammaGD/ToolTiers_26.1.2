package elocindev.tierify.util;

public final class CombatContextHelper {

    private static final ThreadLocal<Boolean> VANILLA_MELEE_CRIT = ThreadLocal.withInitial(() -> false);

    private CombatContextHelper() {
    }

    public static void beginMeleeAttack() {
        VANILLA_MELEE_CRIT.set(false);
    }

    public static void markVanillaMeleeCrit(boolean vanillaCrit) {
        VANILLA_MELEE_CRIT.set(vanillaCrit);
    }

    public static boolean wasVanillaMeleeCrit() {
        return VANILLA_MELEE_CRIT.get();
    }

    public static void endMeleeAttack() {
        VANILLA_MELEE_CRIT.remove();
    }
}
