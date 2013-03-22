/**
 * Copyright (c) 2000-2013 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.so.portlet.bookmarks.social;

import com.liferay.compat.portal.service.ServiceContext;
import com.liferay.portal.kernel.portlet.LiferayPortletRequest;
import com.liferay.portal.kernel.util.HtmlUtil;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portlet.asset.model.AssetRenderer;
import com.liferay.portlet.bookmarks.model.BookmarksEntry;
import com.liferay.portlet.bookmarks.service.BookmarksEntryLocalServiceUtil;
import com.liferay.portlet.social.model.SocialActivity;
import com.liferay.so.activities.model.BaseSocialActivityInterpreter;
import com.liferay.so.util.Time;

import java.io.IOException;

import java.net.HttpURLConnection;
import java.net.URL;

import java.text.Format;

import java.util.Date;

/**
 * @author Evan Thibodeau
 */
public class BookmarksActivityInterpreter
	extends BaseSocialActivityInterpreter {

	public String[] getClassNames() {
		return _CLASS_NAMES;
	}

	@Override
	protected String getBody(
			SocialActivity activity, ServiceContext serviceContext)
		throws Exception {

		AssetRenderer assetRenderer = getAssetRenderer(activity);

		BookmarksEntry entry = BookmarksEntryLocalServiceUtil.getEntry(
			activity.getClassPK());

		String bookmarkLink = wrapLink(entry.getUrl(), entry.getName());

		String faviconUrl = HttpUtil.getDomain(entry.getUrl()) + "/favicon.ico";

		LiferayPortletRequest liferayPortletRequest =
			serviceContext.getLiferayPortletRequest();

		if (ping(faviconUrl)) {
			bookmarkLink = wrapLink(
				entry.getUrl(), faviconUrl, entry.getName());
		}
		else if (Validator.isNotNull(
					assetRenderer.getIconPath(liferayPortletRequest))) {

			bookmarkLink = wrapLink(
				getLinkURL(activity, serviceContext),
				assetRenderer.getIconPath(liferayPortletRequest),
				HtmlUtil.escape(
					assetRenderer.getTitle(serviceContext.getLocale())));
		}

		StringBundler sb = new StringBundler(5);

		sb.append("<div class=\"activity-body\"><div class=\"title\">");
		sb.append(bookmarkLink);
		sb.append("</div><div class='bookmarks-page-content'>");
		sb.append(entry.getDescription());
		sb.append("</div></div>");

		return sb.toString();
	}

	@Override
	protected String getLink(
			SocialActivity activity, ServiceContext serviceContext)
		throws Exception {

		return wrapLink(
			getLinkURL(activity, serviceContext),
			serviceContext.translate("view-bookmarks"));
	}

	@Override
	protected String getTitle(
			SocialActivity activity, ServiceContext serviceContext)
		throws Exception {

		String userName = getUserName(activity.getUserId(), serviceContext);

		Format dateFormatDate = getFormatDateTime(
			serviceContext.getLocale(), serviceContext.getTimeZone());

		StringBundler sb = new StringBundler(10);

		sb.append("<div class=\"activity-header\">");
		sb.append("<div class=\"activity-time\" title=\"");
		sb.append(dateFormatDate.format(new Date(activity.getCreateDate())));
		sb.append("\">");
		sb.append(
			Time.getRelativeTimeSpan(
				activity.getCreateDate(), serviceContext.getLocale(),
				serviceContext.getTimeZone()));
		sb.append("</div><div class=\"activity-user-name\">");

		if (activity.getGroupId() != serviceContext.getScopeGroupId()) {
			String groupName = getGroupName(
				activity.getGroupId(), serviceContext);

			Object[] titleArguments = new Object[] {userName, groupName};

			sb.append(serviceContext.translate("x-in-x", titleArguments));
		}
		else {
			sb.append(userName);
		}

		sb.append("</div></div><div class=\"activity-action\">");

		int activityType = activity.getType();

		String actionPattern = null;

		if (activityType == _ADD_ENTRY) {
			actionPattern = "added-a-new-bookmark";
		}
		else if (activityType == _UPDATE_ENTRY) {
			actionPattern = "updated-a-bookmark";
		}

		sb.append(serviceContext.translate(actionPattern));
		sb.append("</div>");

		return sb.toString();

	}

	protected boolean ping(String url) {
		url = url.replaceFirst("https", "http");

		try {
			URL connectionUrl = new URL(url);

			HttpURLConnection connection =
				(HttpURLConnection)connectionUrl.openConnection();

			connection.setConnectTimeout(500);
			connection.setReadTimeout(500);
			connection.setRequestMethod("HEAD");

			int responseCode = connection.getResponseCode();

			if ((responseCode >= 200) && (responseCode <= 399)) {
				return true;
			}
		}
		catch (IOException exception) {
		}

		return false;
	}

	private static final int _ADD_ENTRY = 1;

	private static final String[] _CLASS_NAMES = new String[] {
		BookmarksEntry.class.getName()
	};

	private static final int _UPDATE_ENTRY = 2;

}