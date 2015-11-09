package com.bekvon.bukkit.residence.shopStuff;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import com.bekvon.bukkit.residence.CommentedYamlConfiguration;
import com.bekvon.bukkit.residence.NewLanguage;
import com.bekvon.bukkit.residence.Residence;

public class ShopSignUtil {

    static ConcurrentHashMap<String, List<ShopVote>> VoteList = new ConcurrentHashMap<String, List<ShopVote>>();
    static List<Board> AllBoards = new ArrayList<Board>();

    public void setVoteList(ConcurrentHashMap<String, List<ShopVote>> VoteList) {
	ShopSignUtil.VoteList = VoteList;
    }

    public static ConcurrentHashMap<String, List<ShopVote>> GetAllVoteList() {
	return VoteList;
    }

    public void removeVoteList(String resName) {
	VoteList.remove(resName);
    }

    public static void addVote(String ResName, List<ShopVote> ShopVote) {
	VoteList.put(ResName, ShopVote);
    }

    public void setAllSigns(List<Board> AllBoards) {
	ShopSignUtil.AllBoards = AllBoards;
    }

    public static List<Board> GetAllBoards() {
	return AllBoards;
    }

    public void removeBoard(Board Board) {
	AllBoards.remove(Board);
    }

    public static void addBoard(Board Board) {
	AllBoards.add(Board);
    }

    // Res Shop vote file
    public static void LoadShopVotes() {
	GetAllVoteList().clear();
	File file = new File(Residence.instance.getDataFolder(), "ShopVotes.yml");
	YamlConfiguration f = YamlConfiguration.loadConfiguration(file);

	if (!f.isConfigurationSection("ShopVotes"))
	    return;

	ConfigurationSection ConfCategory = f.getConfigurationSection("ShopVotes");
	ArrayList<String> categoriesList = new ArrayList<String>(ConfCategory.getKeys(false));
	if (categoriesList.size() == 0)
	    return;

	for (String category : categoriesList) {
	    List<String> List = ConfCategory.getStringList(category);
	    List<ShopVote> VoteList = new ArrayList<ShopVote>();
	    for (String oneEntry : List) {
		if (!oneEntry.contains("%"))
		    continue;

		String name = oneEntry.split("%")[0];
		int vote = -1;

		try {
		    vote = Integer.parseInt(oneEntry.split("%")[1]);
		} catch (Exception ex) {
		    continue;
		}
		if (vote < 0)
		    vote = 0;
		else if (vote > 10)
		    vote = 10;
		VoteList.add(new ShopVote(name, vote));

	    }
	    addVote(category, VoteList);
	}
	return;
    }

    // Signs save file
    public static void saveShopVotes() {
	File f = new File(Residence.instance.getDataFolder(), "ShopVotes.yml");
	YamlConfiguration conf = YamlConfiguration.loadConfiguration(f);

	CommentedYamlConfiguration writer = new CommentedYamlConfiguration();
	conf.options().copyDefaults(true);

	writer.addComment("ShopVotes", "DO NOT EDIT THIS FILE BY HAND!");

	if (!conf.isConfigurationSection("ShopVotes"))
	    conf.createSection("ShopVotes");

	for (Entry<String, List<ShopVote>> one : GetAllVoteList().entrySet()) {

	    String path = "ShopVotes." + one.getKey();

	    List<String> list = new ArrayList<String>();

	    for (ShopVote oneVote : one.getValue()) {
		list.add(oneVote.getName() + "%" + oneVote.getVote());
	    }
	    writer.set(path, list);
	}

	try {
	    writer.save(f);
	} catch (IOException e) {
	    e.printStackTrace();
	}
	return;
    }

    // Res Shop vote file
    public static Vote getAverageVote(String resName) {

	ConcurrentHashMap<String, List<ShopVote>> allvotes = GetAllVoteList();

	if (!allvotes.containsKey(resName))
	    return new Vote(5.0, 0);

	List<ShopVote> votes = allvotes.get(resName);

	double total = 0;
	for (ShopVote oneVote : votes) {
	    total += oneVote.getVote();
	}

	double vote = ((int) ((total / votes.size()) * 100)) / 100.0;

	return new Vote(vote, votes.size());
    }

    public static Map<Shops, Double> getSortedShopList() {

	Map<Shops, Double> allvotes = new HashMap<Shops, Double>();

	Map<String, Shops> shops = Residence.getResidenceManager().getShops();

	for (Entry<String, Shops> one : shops.entrySet()) {
	    Vote vote = ShopSignUtil.getAverageVote(one.getKey());
	    allvotes.put(one.getValue(), vote.getVote());
	}

	allvotes = sortByComparator(allvotes);

	return allvotes;
    }

    private static Map<Shops, Double> sortByComparator(Map<Shops, Double> unsortMap) {

	List<Map.Entry<Shops, Double>> list = new LinkedList<Map.Entry<Shops, Double>>(unsortMap.entrySet());

	Collections.sort(list, new Comparator<Map.Entry<Shops, Double>>() {
	    public int compare(Map.Entry<Shops, Double> o1, Map.Entry<Shops, Double> o2) {
		return (o2.getValue()).compareTo(o1.getValue());
	    }
	});
	Map<Shops, Double> sortedMap = new LinkedHashMap<Shops, Double>();
	for (Iterator<Map.Entry<Shops, Double>> it = list.iterator(); it.hasNext();) {
	    Map.Entry<Shops, Double> entry = it.next();
	    sortedMap.put(entry.getKey(), entry.getValue());
	}
	return sortedMap;
    }

    // Shop Sign file
    public static void LoadSigns() {
	GetAllBoards().clear();
	File file = new File(Residence.instance.getDataFolder(), "ShopSigns.yml");
	YamlConfiguration f = YamlConfiguration.loadConfiguration(file);

	if (!f.isConfigurationSection("ShopSigns"))
	    return;

	ConfigurationSection ConfCategory = f.getConfigurationSection("ShopSigns");
	ArrayList<String> categoriesList = new ArrayList<String>(ConfCategory.getKeys(false));
	if (categoriesList.size() == 0)
	    return;
	for (String category : categoriesList) {
	    ConfigurationSection NameSection = ConfCategory.getConfigurationSection(category);
	    Board newTemp = new Board();
	    newTemp.setStartPlace(NameSection.getInt("StartPlace"));
	    newTemp.setWorld(NameSection.getString("World"));
	    newTemp.setTX(NameSection.getInt("TX"));
	    newTemp.setTY(NameSection.getInt("TY"));
	    newTemp.setTZ(NameSection.getInt("TZ"));
	    newTemp.setBX(NameSection.getInt("BX"));
	    newTemp.setBY(NameSection.getInt("BY"));
	    newTemp.setBZ(NameSection.getInt("BZ"));

	    newTemp.GetTopLocation();
	    newTemp.GetBottomLocation();

	    newTemp.GetLocations();

	    addBoard(newTemp);
	}
	return;
    }

    // Signs save file
    public static void saveSigns() {
	File f = new File(Residence.instance.getDataFolder(), "ShopSigns.yml");
	YamlConfiguration conf = YamlConfiguration.loadConfiguration(f);

	CommentedYamlConfiguration writer = new CommentedYamlConfiguration();
	conf.options().copyDefaults(true);

	writer.addComment("ShopSigns", "DO NOT EDIT THIS FILE BY HAND!");

	if (!conf.isConfigurationSection("ShopSigns"))
	    conf.createSection("ShopSigns");

	int cat = 0;
	for (Board one : GetAllBoards()) {
	    cat++;
	    String path = "ShopSigns." + cat;
	    writer.set(path + ".StartPlace", one.GetStartPlace());
	    writer.set(path + ".World", one.GetWorld());
	    writer.set(path + ".TX", one.GetTopLocation().getBlockX());
	    writer.set(path + ".TY", one.GetTopLocation().getBlockY());
	    writer.set(path + ".TZ", one.GetTopLocation().getBlockZ());
	    writer.set(path + ".BX", one.GetBottomLocation().getBlockX());
	    writer.set(path + ".BY", one.GetBottomLocation().getBlockY());
	    writer.set(path + ".BZ", one.GetBottomLocation().getBlockZ());
	}

	try {
	    writer.save(f);
	} catch (IOException e) {
	    e.printStackTrace();
	}
	return;
    }

    public static boolean BoardUpdate() {

	for (Board board : GetAllBoards()) {
	    List<Location> SignsLocation = board.GetLocations();

	    ArrayList<Shops> ShopNames = new ArrayList<Shops>(ShopSignUtil.getSortedShopList().keySet());

	    int Start = board.GetStartPlace();
	    for (Location OneSignLoc : SignsLocation) {

		Block block = OneSignLoc.getBlock();

		if (!(block.getState() instanceof Sign))
		    continue;

		Shops Shop = null;
		if (Residence.getResidenceManager().getShops().size() >= Start)
		    Shop = Residence.getResidenceManager().getShops().get(ShopNames.get(Start - 1).getRes().getName());

		Sign sign = (Sign) block.getState();

		Vote vote = null;
		String votestat = "";
		if (Residence.getResidenceManager().getShops().size() >= Start) {
		    vote = ShopSignUtil.getAverageVote(ShopNames.get(Start - 1).getRes().getName());
		    votestat = vote.getAmount() == 0 ? "" : NewLanguage.getMessage("Language.Shop.SignLines.4").replace("%1", String.valueOf(vote.getVote())).replace(
			"%2", String.valueOf(vote.getAmount()));
		}

		if (Shop != null) {
		    sign.setLine(0, NewLanguage.getMessage("Language.Shop.SignLines.1").replace("%1", String.valueOf(Start)));
		    sign.setLine(1, NewLanguage.getMessage("Language.Shop.SignLines.2").replace("%1", Shop.getRes().getName()));
		    sign.setLine(2, NewLanguage.getMessage("Language.Shop.SignLines.3").replace("%1", Shop.getRes().getOwner()));
		    sign.setLine(3, votestat);
		    board.addSignLoc(Shop.getRes().getName(), sign.getLocation());
		} else {
		    sign.setLine(0, "");
		    sign.setLine(1, "");
		    sign.setLine(2, "");
		    sign.setLine(3, "");
		}
		sign.update();

		Start++;
	    }
	}

	return true;
    }
}
