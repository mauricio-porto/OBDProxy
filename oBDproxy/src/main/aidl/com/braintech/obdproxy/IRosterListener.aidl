package com.braintech.obdproxy;

import com.braintech.obdproxy.RosterEntryInfo;

interface IRosterListener {
	void entryAdded(in RosterEntryInfo contact);
	void entryRemoved(in RosterEntryInfo contact);
	void statusChanged(in RosterEntryInfo contact);
}
