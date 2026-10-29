package com.financetracker.session;

import com.vaadin.flow.server.VaadinSession;

public final class AppSession {

	private static final String ENTERED = "steady.entered";
	private static final String DEMO = "steady.demo";
	private static final String DISPLAY_NAME = "steady.name";

	private AppSession() {
	}

	public static boolean isEntered() {
		Boolean entered = attribute(ENTERED);
		return Boolean.TRUE.equals(entered);
	}

	public static boolean isDemoMode() {
		Boolean demo = attribute(DEMO);
		return Boolean.TRUE.equals(demo);
	}

	public static String displayName() {
		String name = attribute(DISPLAY_NAME);
		return name == null || name.isBlank() ? "there" : name.trim();
	}

	public static void enter(String displayName, boolean demoMode) {
		VaadinSession session = VaadinSession.getCurrent();
		session.setAttribute(ENTERED, Boolean.TRUE);
		session.setAttribute(DEMO, demoMode);
		session.setAttribute(DISPLAY_NAME, displayName == null ? "" : displayName.trim());
	}

	public static void clear() {
		VaadinSession session = VaadinSession.getCurrent();
		session.setAttribute(ENTERED, Boolean.FALSE);
		session.setAttribute(DEMO, Boolean.FALSE);
		session.setAttribute(DISPLAY_NAME, "");
	}

	@SuppressWarnings("unchecked")
	private static <T> T attribute(String key) {
		VaadinSession session = VaadinSession.getCurrent();
		if (session == null) {
			return null;
		}
		return (T) session.getAttribute(key);
	}
}
