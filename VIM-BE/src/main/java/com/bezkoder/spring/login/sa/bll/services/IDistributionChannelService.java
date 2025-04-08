package com.bezkoder.spring.login.sa.bll.services;

import java.util.List;

import com.bezkoder.spring.login.sa.dal.entities.CfgTblDistributionChannel;


public interface IDistributionChannelService {

	List<CfgTblDistributionChannel> getAllDistributionChannel();
	
	List<CfgTblDistributionChannel> getActiveDistributionChannel();
	
	String addNewDistributionChannel(CfgTblDistributionChannel cfgTblDistributionChannel);

	boolean distributionChannelExistByProperty(String property, String value, String mode, String oldValue);
	
	String deleteDistributionChannel(List<String> distributionChannelsId);

	String updateDistributionChannel(CfgTblDistributionChannel cfgTblDistributionChannel);
	
	String generateDistributionChannelNo(String type);
	
	List<CfgTblDistributionChannel> searchDistributionChannel(CfgTblDistributionChannel distributionChannel);
	

}
