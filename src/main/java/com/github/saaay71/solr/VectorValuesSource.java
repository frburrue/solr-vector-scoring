package com.github.saaay71.solr;

import org.apache.lucene.analysis.payloads.PayloadHelper;
import org.apache.lucene.index.*;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.DoubleValues;
import org.apache.lucene.search.DoubleValuesSource;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VectorValuesSource extends DoubleValuesSource {

    private final String field;
    private List<Double> vector;
    private double queryVectorNorm;
    private boolean cosine;

    private Terms terms;
    private TermsEnum te;

    public VectorValuesSource(String field, String Vector, boolean cosine) {
        this.field = field;
        this.vector = new ArrayList<>();
        this.cosine = cosine;
        String[] vectorArray = Vector.split(",");
        for (String s : vectorArray) {
            double v = Double.parseDouble(s);
            vector.add(v);
            if (cosine) {
                queryVectorNorm += Math.pow(v, 2.0);
            }
        }
    }

    public DoubleValues getValues(LeafReaderContext leafReaderContext, DoubleValues doubleValues) throws IOException {

        LeafReader reader = leafReaderContext.reader();

        return new DoubleValues() {

            public double doubleValue() throws IOException {
                double docVectorNorm = 0.0;
                double score = 0;
                BytesRef text;
                while ((text = te.next()) != null) {
                    String term = text.utf8ToString();
                    if (term.isEmpty()) {
                        continue;
                    }
                    float payloadValue = 0f;
                    PostingsEnum postings = te.postings(null, PostingsEnum.ALL);
                    while (postings.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
                        int freq = postings.freq();
                        while (freq-- > 0) postings.nextPosition();

                        BytesRef payload = postings.getPayload();
                        payloadValue = PayloadHelper.decodeFloat(payload.bytes, payload.offset);

                        if (cosine)
                            docVectorNorm += Math.pow(payloadValue, 2.0);
                    }

                    score = (score + payloadValue * (vector.get(Integer.parseInt(term))));
                }

                if (cosine) {
                    if ((docVectorNorm == 0) || (queryVectorNorm == 0)) score = 0f;
                    score = (float)(score / (Math.sqrt(docVectorNorm) * Math.sqrt(queryVectorNorm)));
                }
                return score;
            }

            public boolean advanceExact(int doc) throws IOException {
                terms = reader.getTermVector(doc, field);
                if (terms == null) {
                    return false;
                }
                te = terms.iterator();
                return true;
            }
        };
    }

    public boolean needsScores() {
        return true;
    }

    public DoubleValuesSource rewrite(IndexSearcher indexSearcher) throws IOException {
        return this;
    }

    public int hashCode() {
        return 0;
    }

    public boolean equals(Object o) {
        return false;
    }

    public String toString() {
        return cosine ? "cosine(" + field + ",doc)" : "dot-product(" + field + ",doc)";
    }

    public boolean isCacheable(LeafReaderContext leafReaderContext) {
        return false;
    }
}
