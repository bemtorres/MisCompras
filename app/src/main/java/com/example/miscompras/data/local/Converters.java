package com.example.miscompras.data.local;

import androidx.room.TypeConverter;

import com.example.miscompras.domain.DiscountType;
import com.example.miscompras.domain.ListStatus;
import com.example.miscompras.domain.ListType;

public class Converters {

    @TypeConverter
    public String fromListType(ListType type) {
        return type == null ? ListType.WEEKLY.name() : type.name();
    }

    @TypeConverter
    public ListType toListType(String value) {
        return ListType.fromName(value);
    }

    @TypeConverter
    public String fromListStatus(ListStatus status) {
        return status == null ? ListStatus.PLANNED.name() : status.name();
    }

    @TypeConverter
    public ListStatus toListStatus(String value) {
        return ListStatus.fromName(value);
    }

    @TypeConverter
    public String fromDiscountType(DiscountType type) {
        return type == null ? DiscountType.NONE.name() : type.name();
    }

    @TypeConverter
    public DiscountType toDiscountType(String value) {
        return DiscountType.fromName(value);
    }
}
