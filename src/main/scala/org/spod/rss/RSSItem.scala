package org.spod.rss

import java.net.URL
import java.time.Instant

case class RSSItem(
    title: String,
    description: String,
    link: URL,
    publicationDate: Instant
)
