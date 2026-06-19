package com.pixellink.mapper;

import com.pixellink.model.Payment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface PaymentMapper {
    Payment findByLinkIdAndIpHash(@Param("linkId") String linkId, @Param("ipHash") String ipHash);
    void insert(Payment payment);
    List<Payment> findAll();
    List<Payment> findByLinkId(String linkId);
}
