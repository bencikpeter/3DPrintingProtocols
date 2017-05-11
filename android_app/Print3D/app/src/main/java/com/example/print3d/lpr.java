package com.example.print3d;

import com.example.print3d.jLpr;
import java.io.*;
import java.net.*;


class lpr {
	public static void main(String args[]) {
		try {
			if (args.length != 3) {
				System.out.println("Useage: lpr HostName PrinterName PrintFile");
				return;
			}
			jLpr myLpr = new jLpr();
			myLpr.setPrintRaw(true);
			myLpr.setUseOutOfBoundPorts(true);
			myLpr.printFile(args[2], args[0], args[1]);
			System.out.println("Printed");
		}
		catch (Exception e) {
			System.out.println(e);
		}
	}
}