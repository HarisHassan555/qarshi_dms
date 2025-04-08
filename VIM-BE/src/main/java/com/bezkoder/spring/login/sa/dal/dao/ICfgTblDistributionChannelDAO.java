package com.bezkoder.spring.login.sa.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblDistributionChannel;

public interface ICfgTblDistributionChannelDAO {

	List<CfgTblDistributionChannel> getAllDistributionChannel();

	List<CfgTblDistributionChannel> getActiveDistributionChannel();

	List<CfgTblDistributionChannel> getDistributionChannelByProperty(String property, String value, String mode, String oldValue);

	String addNewDistributionChannel(CfgTblDistributionChannel cfgTblDistributionChannel);

	String deleteDistributionChannel(List<String> customerId);

	String updateDistributionChannel(CfgTblDistributionChannel cfgTblDistributionChannel);

	String generateDistributionChannelNo(String type);

	String getDistributionChannelById(String customerId);
	
	List<CfgTblDistributionChannel> searchDistributionChannel(CfgTblDistributionChannel country);
}
