package codechicken.multipart.scalatraits;

import java.util.Random;

import codechicken.multipart.IRandomDisplayTick;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;
import codechicken.multipart.TileMultipartClient;
import scala.collection.Iterator;

/** Client mixin that dispatches random display ticks only to interested parts. */
public class TRandomDisplayTickTile extends TileMultipartClient {

    @Override
    public void randomDisplayTick(Random random) {
        TRandomDisplayTickTileAccess.randomDisplayTick(this, random);
    }
}

/** Keeps inherited part-list access outside the generated trait transformer. */
final class TRandomDisplayTickTileAccess {

    private TRandomDisplayTickTileAccess() {}

    static void randomDisplayTick(Object tile, Random random) {
        Iterator<TMultiPart> parts = ((TileMultipart) tile).partList().iterator();
        while (parts.hasNext()) {
            TMultiPart part = parts.next();
            if (part instanceof IRandomDisplayTick) {
                ((IRandomDisplayTick) part).randomDisplayTick(random);
            }
        }
    }
}
