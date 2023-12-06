package cn.ac.iie.pkcgroup.dws.core.db.model;

public enum ConstraintType {
    INTEGER {
        @Override
        public boolean isNumeric() {
            return true;
        }
    }, // 整型
    DOUBLE {
        @Override
        public boolean isNumeric() {
            return true;
        }
    }, //浮点型
    TEXT {
        @Override
        public boolean isNumeric() {
            return false;
        }
    }; //文本型

    public abstract boolean isNumeric();
}
