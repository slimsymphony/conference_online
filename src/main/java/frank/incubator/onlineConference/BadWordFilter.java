package frank.incubator.onlineConference;

import org.cometd.server.filter.DataFilter;
import org.cometd.server.filter.NoMarkupFilter;

public class BadWordFilter extends NoMarkupFilter {
	 @Override
     protected Object filterString(String string)
     {
         if (string.indexOf("fuck")>=0)
             throw new DataFilter.Abort();
         return string;
     }
}
