package org.jzy3d.ui;

import java.awt.Rectangle;
import java.lang.reflect.InvocationTargetException;

import org.jzy3d.chart.Chart;

public class Plugs {

	public static void frame(Chart chart){
		try {
    		Class.forName("org.jzy3d.bridge.PlugsImpl")
                    .getMethod("frame", Chart.class)
                    .invoke(null, chart);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
    }
	
	public static void frame(Chart chart, Rectangle bounds, String title){
		try {
            Class.forName("org.jzy3d.bridge.PlugsImpl")
                    .getMethod("frame", Chart.class, Rectangle.class, String.class)
                    .invoke(null, chart, bounds, title);
        } catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
