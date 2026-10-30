package mn.lls.tiletimer;

/** Quick Settings tile that asks for a time instead of using a preset. */
public class TileCustomService extends BaseTileService {
    @Override
    protected int slot() {
        return Prefs.CUSTOM_SLOT;
    }
}
