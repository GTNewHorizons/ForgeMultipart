package codechicken.multipart.scalatraits;

import codechicken.multipart.JPartialOcclusion;
import codechicken.multipart.PartialOcclusionTest;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;
import scala.collection.Seq;
import scala.collection.Seq$;
import scala.collection.mutable.Builder;

/** Implementation for the partial occlusion test. */
public class TPartialOcclusionTile extends TileMultipart {

    @Override
    public boolean occlusionTest(Seq<TMultiPart> parts, TMultiPart newPart) {
        if (newPart instanceof JPartialOcclusion) {
            Builder<TMultiPart, Seq<TMultiPart>> builder = Seq$.MODULE$.newBuilder();
            builder.$plus$plus$eq(parts);
            builder.$plus$eq(newPart);
            if (!partialOcclusionTest(builder.result())) {
                return false;
            }
        }
        return super.occlusionTest(parts, newPart);
    }

    public boolean partialOcclusionTest(Seq<TMultiPart> parts) {
        PartialOcclusionTest test = new PartialOcclusionTest(parts.length());
        for (int index = 0; index < parts.length(); index++) {
            TMultiPart part = parts.apply(index);
            if (part instanceof JPartialOcclusion) {
                test.fill(index, (JPartialOcclusion) part);
            }
        }
        return test.apply();
    }
}
