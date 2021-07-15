package de.tudarmstadt.peasec.entity.help;

import de.tudarmstadt.peasec.entity.ProcessedTextEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Cluster {
    private Map<Long, ProcessedTextEntity> entityMap = new HashMap<>();
    private Map<Long, TokenVector> tokenVectorMap = new HashMap<>();

    private TokenVector center = null;

    private Map<String, Integer> dfMap;

    public Cluster() {
    }

    public void setDfMap(Map<String, Integer> map) {
        this.dfMap = map;
    }

    public TokenVector getCenter() {
        if(this.center == null) {
            TokenVector c = TokenVector.getCenter(new ArrayList<>(tokenVectorMap.values()));
            this.center = c.multiplyIdf(dfMap);
        }
        return this.center;
    }

    public void add(ProcessedTextEntity e) {
        entityMap.put(e.getTweetId(), e);
        tokenVectorMap.put(e.getTweetId(), new TokenVector(e.getText()));
        this.center = null;
    }

    public void delete(long id) {
        entityMap.remove(id);
        tokenVectorMap.remove(id);
        this.center = null;
    }

    public List<ProcessedTextEntity> getProcessedTextEntityList() {
        return new ArrayList<>(this.entityMap.values());
    }

    public int getEntityCount() {
        return this.entityMap.size();
    }
}
