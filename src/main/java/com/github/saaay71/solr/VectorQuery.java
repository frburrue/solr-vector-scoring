package com.github.saaay71.solr;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.*;

import java.io.IOException;

public class VectorQuery extends Query {
	String queryStr = "";
	Query q;

	public VectorQuery(Query subQuery) {
		this.q = subQuery;
	}
	
	public void setQueryString(String queryString){
		this.queryStr = queryString;
	}

	@Override
	public Weight createWeight(IndexSearcher searcher, boolean needsScores, float boost) throws IOException {
		Weight w;
		if(q == null){
			w =  new ConstantScoreWeight(this, boost) {
				@Override
				public Scorer scorer(LeafReaderContext context) throws IOException {
					return new ConstantScoreScorer(this, score(), DocIdSetIterator.all(context.reader().maxDoc()));
				}
			};
		}else{
			w = searcher.createWeight(q, needsScores, boost);
		}
		return w;
	}

	@Override
	public String toString(String field) {
		return queryStr;
	}

	@Override
	public boolean equals(Object other) {
		return sameClassAs(other) &&
				queryStr.equals(other.toString());
	}

	@Override
	public int hashCode() {
		return classHash() ^ queryStr.hashCode();
	}

}
