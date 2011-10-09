package com.massivecraft.factions.integration;

import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;

import com.massivecraft.factions.listeners.FactionsServerListener;
import com.massivecraft.factions.cmd.CmdHelp;

import com.earth2me.essentials.api.Economy;
import com.nijikokun.register.payment.Methods;
import com.nijikokun.register.payment.Method.MethodAccount;
import com.iConomy.*;
import com.iConomy.system.*;
import com.massivecraft.factions.Conf;
import com.massivecraft.factions.P;


public class Econ {
	private static boolean registerUse = false;
	private static boolean iConomyUse = false;
	private static boolean essEcoUse = false;

	public static void monitorPlugins() {
		P.p.getServer().getPluginManager().registerEvent(Event.Type.PLUGIN_ENABLE, new FactionsServerListener(P.p), Event.Priority.Monitor, P.p);
		P.p.getServer().getPluginManager().registerEvent(Event.Type.PLUGIN_DISABLE, new FactionsServerListener(P.p), Event.Priority.Monitor, P.p);
	}

	public static void setup(P factions) {
		if (enabled()) {
			return;
		}

		if (!registerHooked()) {
			Plugin plug = factions.getServer().getPluginManager().getPlugin("Register");
			if (plug != null && plug.getClass().getName().equals("com.nijikokun.register.Register") && plug.isEnabled()) {
				registerSet(true);
			}
		}
		if (!iConomyHooked()) {
			Plugin plug = factions.getServer().getPluginManager().getPlugin("iConomy");
			if (plug != null && plug.getClass().getName().equals("com.iConomy.iConomy") && plug.isEnabled()) {
				iConomySet(true);
			}
		}
		if (!essentialsEcoHooked()) {
			Plugin plug = factions.getServer().getPluginManager().getPlugin("Essentials");
			if (plug != null && plug.isEnabled()) {
				essentialsEcoSet(true);
			}
		}
	}

	public static void registerSet(boolean enable)
	{
		registerUse = enable;
		if (enable) {
			P.p.log("Register hook available, "+(Conf.econRegisterEnabled ? "and interface is enabled" : "but disabled (\"econRegisterEnabled\": false)")+".");
		}
		else {
			P.p.log("Un-hooked from Register.");
		}
		CmdHelp.updateHelp();
	}

	public static void iConomySet(boolean enable)
	{
		iConomyUse = enable;
		if (enable && !registerUse) {
			P.p.log("iConomy hook available, "+(Conf.econIConomyEnabled ? "and interface is enabled" : "but disabled (\"econIConomyEnabled\": false)")+".");
		}
		else {
			P.p.log("Un-hooked from iConomy.");
		}
		CmdHelp.updateHelp();
	}

	public static void essentialsEcoSet(boolean enable)
	{
		essEcoUse = enable;
		if (enable && !registerUse)
		{
			P.p.log("EssentialsEco hook available, "+(Conf.econEssentialsEcoEnabled ? "and interface is enabled" : "but disabled (\"econEssentialsEcoEnabled\": false)")+".");
		}
		else
		{
			P.p.log("Un-hooked from EssentialsEco.");
		}
		CmdHelp.updateHelp();
	}

	public static boolean registerHooked()
	{
		return registerUse;
	}

	public static boolean iConomyHooked()
	{
		return iConomyUse;
	}

	public static boolean essentialsEcoHooked()
	{
		return essEcoUse;
	}

	public static boolean registerAvailable()
	{
		return Conf.econRegisterEnabled && registerUse && Methods.hasMethod();
	}

	// If economy is enabled in conf.json, and we're successfully hooked into an economy plugin
	public static boolean enabled()
	{
		return (Conf.econRegisterEnabled && registerUse && Methods.hasMethod())
			   || (Conf.econIConomyEnabled && iConomyUse)
			   || (Conf.econEssentialsEcoEnabled && essEcoUse);
	}

	// mainly for internal use, for a little less code repetition
	public static Holdings getIconomyHoldings(String playerName)
	{
		if ( ! enabled())
		{
			return null;
		}

		Account account = iConomy.getAccount(playerName);
		if (account == null)
		{
			return null;
		}
		Holdings holdings = account.getHoldings();
		return holdings;
	}
	public static MethodAccount getRegisterAccount(String playerName)
	{
		if (!enabled())
		{
			return null;
		}
		if (!Methods.getMethod().hasAccount(playerName))
		{
			return null;
		}

		MethodAccount account = Methods.getMethod().getAccount(playerName);
		return account;
	}


	// format money string based on server's set currency type, like "24 gold" or "$24.50"
	public static String moneyString(double amount)
	{
		return registerAvailable() ? Methods.getMethod().format(amount)
			   : (iConomyUse ? iConomy.format(amount) : Economy.format(amount));
	}

	// whether a player can afford specified amount
	public static boolean canAfford(String playerName, double amount) {
		// if Economy support is not enabled, they can certainly afford to pay nothing
		if (!enabled())
		{
			return true;
		}

		if (registerAvailable())
		{
			MethodAccount holdings = getRegisterAccount(playerName);
			if (holdings == null)
			{
				return false;
			}

			return holdings.hasEnough(amount);
		}
		else if (iConomyUse)
		{
			Holdings holdings = getIconomyHoldings(playerName);
			if (holdings == null)
			{
				return false;
			}

			return holdings.hasEnough(amount);
		}
		else
		{
			try
			{
				return Economy.hasEnough(playerName, amount);
			}
			catch (Exception ex)
			{
				return false;
			}
		}
	}

	// deduct money from their account; returns true if successful
	public static boolean deductMoney(String playerName, double amount)
	{
		if (!enabled())
		{
			return true;
		}

		if (registerAvailable())
		{
			MethodAccount holdings = getRegisterAccount(playerName);
			if (holdings == null || !holdings.hasEnough(amount))
			{
				return false;
			}

			return holdings.subtract(amount);
		}
		else if (iConomyUse)
		{
			Holdings holdings = getIconomyHoldings(playerName);
			if (holdings == null || !holdings.hasEnough(amount))
			{
				return false;
			}

			holdings.subtract(amount);
			return true;
		}
		else
		{
			try
			{
				if (!Economy.hasEnough(playerName, amount))
				{
					return false;
				}
				Economy.subtract(playerName, amount);
				return true;
			}
			catch (Exception ex)
			{
				return false;
			}
		}
	}

	// add money to their account; returns true if successful
	public static boolean addMoney(String playerName, double amount)
	{
		if (!enabled())
		{
			return true;
		}

		if (registerAvailable())
		{
			MethodAccount holdings = getRegisterAccount(playerName);
			if (holdings == null)
			{
				return false;
			}

			return holdings.add(amount);
		}
		else if (iConomyUse) 
		{
			Holdings holdings = getIconomyHoldings(playerName);
			if (holdings == null)
			{
				return false;
			}

			holdings.add(amount);
			return true;
		}
		else
		{
			try
			{
				Economy.add(playerName, amount);
				return true;
			}
			catch (Exception ex)
			{
				return false;
			}
		}
	}


	// calculate the cost for claiming land
	public static double calculateClaimCost(int ownedLand, boolean takingFromAnotherFaction)
	{
		if (!enabled())
		{
			return 0.0;
		}

		// basic claim cost, plus land inflation cost, minus the potential bonus given for claiming from another faction
		return Conf.econCostClaimWilderness
			+ (Conf.econCostClaimWilderness * Conf.econClaimAdditionalMultiplier * ownedLand)
			- (takingFromAnotherFaction ? Conf.econCostClaimFromFactionBonus: 0);
	}

	// calculate refund amount for unclaiming land
	public static double calculateClaimRefund(int ownedLand)
	{
		return calculateClaimCost(ownedLand - 1, false) * Conf.econClaimRefundMultiplier;
	}

	// calculate value of all owned land
	public static double calculateTotalLandValue(int ownedLand)
	{
		double amount = 0;
		for (int x = 0; x < ownedLand; x++) {
			amount += calculateClaimCost(x, false);
		}
		return amount;
	}

	// calculate refund amount for all owned land
	public static double calculateTotalLandRefund(int ownedLand)
	{
		return calculateTotalLandValue(ownedLand) * Conf.econClaimRefundMultiplier;
	}
}
