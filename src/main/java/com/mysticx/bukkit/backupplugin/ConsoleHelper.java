package com.mysticx.bukkit.backupplugin;

/*
 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Lesser General Public License as published
 by the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

import java.lang.reflect.Field;
import java.util.logging.Level;

import org.bukkit.Server;
import org.bukkit.craftbukkit.CraftServer;
import net.minecraft.server.MinecraftServer;

public class ConsoleHelper {

    public static boolean queueConsoleCommand(Server server, String cmd) {

        try {
            Field f = CraftServer.class.getDeclaredField("console");
            f.setAccessible(true);
            MinecraftServer ms = (MinecraftServer) f.get(server);

            if ((!ms.g) && (MinecraftServer.a(ms))) {
                ms.a(cmd, ms);
                return true;
            }

        } catch (Exception e) {
            MessageHandler.log(Level.SEVERE, "error in queueConsoleCommand", e);
        }

        return false;
    }

}
