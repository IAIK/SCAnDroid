# The following code is based on the ProcHarvester implementation
# See https://github.com/IAIK/ProcHarvester/tree/master/code/adb-record-tools

from enum import Enum

RECORDS_PER_APP = 8
RECORDS_PER_WEBSITE = 8
RECORDS_PER_POI_SEARCH = 8

DELAY_AFTER_LAUNCH = 10
DELAY_AFTER_KILL = 2


class RecordMode(Enum):
    COLD_STARTS = 1
    APP_RESUMES = 2
    MIXED_MODE = 3


RECORD_MODE = RecordMode.COLD_STARTS

POPULAR_TARGET_SET = [
    "com.airbnb.android",
    "com.duckduckgo.mobile.android",
    "com.instagram.android",
    "com.isis_papyrus.raiffeisen_pay_eyewdg",
    "com.moshbit.studo",
    "com.paypal.android.p2pmobile",
    "com.tripadvisor.tripadvisor",
    "com.twitter.android",
    "com.waze",
    "com.whatsapp",
    "de.mcdonalds.mcdonaldsinfoapp",
    "de.pilot.newyorker.android",
    "de.prosiebensat1digital.prosieben",
    "de.spiegel.android.app.spon",
    "de.zalando.mobile",
    "org.chromium.chrome",
    "org.indywidualni.fblite",
    "org.mozilla.firefox",
    "org.zwanoo.android.speedtest",
    "com.fsck.k9",
]

DEFAULT_APPS_NOUGAT = [
    "com.google.android.GoogleCamera",
    "com.google.android.apps.docs",
    "com.google.android.apps.messaging",
    "com.google.android.apps.photos",
    "com.google.android.calendar",
    "com.google.android.contacts",
    "com.google.android.deskclock",
    "com.google.android.dialer",
    "com.google.android.gm",
    "com.google.android.launcher",
    "com.google.android.music",
    "com.google.android.talk",
    "com.google.android.videos",
    "com.google.android.youtube",
    "com.google.android.apps.maps",
    "com.google.android.play.games",
    "org.fdroid.fdroid",
    "org.moparisthebest.appbak",
]

OLD_TARGET_SET = [
    "com.fsck.k9",
    "org.chromium.chrome",
    "org.mozilla.fennec_fdroid",
    "org.mozilla.firefox",
    "org.lineageos.jelly",
    "atm.nasaimages",
    "com.duckduckgo.mobile.android",
    "com.nextcloud.client",
    "com.wangdaye.mysplash",
    "com.whatsapp",
    "de.christinecoenen.code.zapp",
    "eu.uwot.fabio.altcoinprices",
    "free.rm.skytube.oss",
    "in.denim.wallsforreddit",
    "it.niedermann.owncloud.notes",
    "net.reichholf.dreamdroid",
    "org.indywidualni.fblite",
    "org.kde.kdeconnect_tp",
    "org.schabi.newpipe",
    "org.torproject.android",
]

TARGET_WEBSITES = [
    "http://www.google.com",
    "http://www.facebook.com",
    "http://www.baidu.com",
    "http://www.wikipedia.org",
    "http://www.yahoo.com",
    "http://www.reddit.com",
    "http://www.qq.com",
    "http://www.amazon.com",
    "http://www.tmall.com",
    "http://www.sohu.com",
    "http://www.live.com",
    "http://www.vk.com",
    "http://www.instagram.com",
    "http://www.sina.com.cn",
    "http://www.360.cn",
    "http://www.jd.com",
    "http://www.linkedin.com",
    "http://www.netflix.com",
    "http://www.imgur.com",
    "http://www.yandex.ru",
]

TARGET_APPS = POPULAR_TARGET_SET


def records_per_app():
    return RECORDS_PER_APP
