package com.vuphone.tictactoe.network;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

public class NetworkManager {

	private WifiManager wifi_ = null;
	private static NetworkManager instance_ = null;
	WifiManager.WifiLock wifiLock = null;
	
	public static NetworkManager getInstance() {
		if (instance_ == null)
			instance_ = new NetworkManager();

		return instance_;
	}

	public void setWifiManager(WifiManager wifi) {
		wifi_ = wifi;
	}

	public boolean isWifiEnabled() {
		if (wifi_ == null)
			return false;

		return wifi_.isWifiEnabled();
	}

	public void setWifiEnabled(boolean enabled) {
		wifi_.setWifiEnabled(enabled);
	}

	public String getSubnetMask() {
		if (wifi_ == null)
			return null;

		DhcpInfo dhcp = wifi_.getDhcpInfo();

		if (dhcp != null)
			return wifiInt2String(dhcp.netmask);

		return null;
	}

	public DhcpInfo getDhcpInfo() {
		return wifi_.getDhcpInfo();
	}

	/**
	 * Gets IP address of Wifi
	 * 
	 * @return
	 */
	public String getIpAddressWifi() {
		int i = wifi_.getConnectionInfo().getIpAddress();

		if (i == 0)
			return null;

		return wifiInt2String(i);
	}

	public void reassociateWifi() {
		wifi_.reassociate();
	}

	private String wifiInt2String(int ip) {
		String str = null;
		byte[] byteaddr = new byte[] { (byte) (ip & 0xff),
				(byte) (ip >> 8 & 0xff), (byte) (ip >> 16 & 0xff),
				(byte) (ip >> 24 & 0xff) };
		try {
			str = InetAddress.getByAddress(byteaddr).getHostAddress();
		} catch (Exception e) {
			// TODO: handle exception
		}

		return str;
	}

	/**
	 * Determines if IP falls in ranges: 10.0.0.0 – 10.255.255.255 | 172.16.0.0
	 * – 172.31.255.255 | 192.168.0.0 – 192.168.255.255
	 * 
	 * @param ip
	 *            - must be valid
	 * @return
	 */
	public boolean isIpInternal(String ip) {
		if (ip == null || !isIpValid(ip))
			return false;

		// 10.0.0.0 – 10.255.255.255
		if (ip.startsWith("10."))
			return true;

		// 172.16.0.0 – 172.31.255.255
		if (ip.startsWith("172.") && ip.charAt(6) == '.') {
			int oct = Integer.parseInt(ip.substring(4, 6));
			if (oct >= 16 && oct <= 31)
				return true;
		}

		// 192.168.0.0 – 192.168.255.255
		if (ip.startsWith("192.168."))
			return true;

		return false;
	}

	/**
	 * Determines if IP is of the form 127.*.*.*
	 * 
	 * @param ip
	 * @return
	 */
	public boolean isIpLoopback(String ip) {
		if (ip == null || ip.length() < 5)
			return false;

		return ip.substring(0, 3).equals("127");
	}

	/**
	 * Determines if IP is in range 0.0.0.0 through 255.255.255.255
	 * 
	 * @param ip
	 * @return
	 */
	public boolean isIpValid(String ip) {
		if (ip == null)
			return false;

		// Check for a valid IP
		Pattern regex = Pattern
				.compile(
						"^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$",
						Pattern.MULTILINE);
		Matcher regexMatcher = regex.matcher(ip);

		return regexMatcher.matches() ? true : false;
	}

	/**
	 * Gets IP address of Wifi or 3G
	 * 
	 * @return
	 */
	public String getIpAddress() {
		String ip = null;
		try {
			Enumeration<NetworkInterface> en = NetworkInterface
					.getNetworkInterfaces();

			while (en.hasMoreElements()) {
				NetworkInterface intf = en.nextElement();

				Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses();

				while (enumIpAddr.hasMoreElements()) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress())
						ip = inetAddress.getHostAddress().toString();

					Log.d("mad", "  Several IPs found: "
							+ inetAddress.getHostAddress().toString());
				}
			}
		} catch (SocketException ex) {
			ex.printStackTrace();
		}

		return ip;
	}

	public boolean isConnectionWorking() {
		return (getIpAddressFromTest() == null) ? false : true;
	}

	public String getIpAddressFromTest() {
		// lets find our IP address
		String ip = null;
		try {
			Socket socket = new Socket("www.google.com", 80);
			ip = socket.getLocalAddress().toString().substring(1);
			socket.close();
		} catch (Exception e) {
			return null;
		}

		if (!isIpValid(ip) || isIpLoopback(ip))
			return null;
		else
			return ip;
	}

	public void createWifiLock(boolean wifi_mode_full) {
		releaseWifiLock();

		wifiLock = wifi_.createWifiLock(wifi_mode_full ? WifiManager.WIFI_MODE_FULL
				: WifiManager.WIFI_MODE_SCAN_ONLY, "TrustiveWifi wifilock");
		wifiLock.setReferenceCounted(false);
		wifiLock.acquire();

	}

	public void releaseWifiLock() {
		if (wifiLock != null && wifiLock.isHeld()) {
			wifiLock.release();
			wifiLock = null;
		}
	}
}
