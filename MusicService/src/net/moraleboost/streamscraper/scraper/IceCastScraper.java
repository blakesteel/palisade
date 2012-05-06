/*
 **
 **  Jul. 20, 2009
 **
 **  The author disclaims copyright to this source code.
 **  In place of a legal notice, here is a blessing:
 **
 **    May you do good and not evil.
 **    May you find forgiveness for yourself and forgive others.
 **    May you share freely, never taking more than you give.
 **
 **                                         Stolen from SQLite :-)
 **  Any feedback is welcome.
 **  Kohei TAKETA <k-tak@void.in>
 **
 */
package net.moraleboost.streamscraper.scraper;

import blakesteel.MusicService.Utility;
import java.net.URI;
import java.util.List;
import net.moraleboost.streamscraper.Parser;
import net.moraleboost.streamscraper.ScrapeException;
import net.moraleboost.streamscraper.Scraper;
import net.moraleboost.streamscraper.Stream;
import net.moraleboost.streamscraper.parser.IceCastParser;

public class IceCastScraper implements Scraper
{
    private Parser parser;
    
    public IceCastScraper()
    {
        //fetcher = new HttpFetcher();
        parser = new IceCastParser();
    }

    public Parser getParser()
    {
        return parser;
    }

    public void setParser(Parser parser)
    {
        this.parser = parser;
    }

    public List<Stream> scrape(URI uri) throws ScrapeException
    {
        try {
            byte[] data = Utility.httpGetBytes(uri.resolve("/status.xsl"));
            return parser.parse(uri, data);
        } catch (Exception e) {
            throw new ScrapeException(e);
        }
    }
}
