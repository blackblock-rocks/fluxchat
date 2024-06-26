package rocks.blackblock.fluxchat.hooks;

import rocks.blackblock.fluxchat.tab.FluxChatTabList;

import java.util.TimerTask;

public class TimerHook extends TimerTask {

    @Override
    public void run() {
        if (FluxChatTabList.instance != null) {
            FluxChatTabList.instance.update();
        }
    }
}
