package blakesteel.MusicService;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;

/**
 * @author Palisade
 */
public class TownInfo {
    public boolean FromWild = false, ToWild = false, ToForSale = false, ToHomeBlock = false;
    public Town FromTown = null, ToTown = null;
    public Resident FromResident = null, ToResident = null;
    public boolean IsTownHome = false;
}