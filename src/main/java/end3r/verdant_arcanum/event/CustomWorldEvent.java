// CustomWorldEvent.java
package end3r.verdant_arcanum.event;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

public interface CustomWorldEvent {
    void start(ServerWorld world);
    void tick(ServerWorld world);
    boolean isComplete();
    Identifier getId();
}
