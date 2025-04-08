package com.bezkoder.spring.login.sa.bll.servicesimpl;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.login.sa.bll.services.IDistributionChannelService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblDistributionChannelDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblDistributionChannel;


@Service
public class DistributionChannelService implements IDistributionChannelService {
	
	@Autowired
	private ICfgTblDistributionChannelDAO citTableDistributionChannelDAO;

	private Logger logger = LogManager.getLogger(DistributionChannelService.class);

	public DistributionChannelService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<CfgTblDistributionChannel> getAllDistributionChannel() {
		logger.debug("getAllDistributionChannels()");
		List<CfgTblDistributionChannel> distributionChannels = citTableDistributionChannelDAO.getAllDistributionChannel();
		return distributionChannels;
	}
	
	@Override
	public List<CfgTblDistributionChannel> getActiveDistributionChannel() {
		logger.debug("getActiveDistributionChannels()");
		List<CfgTblDistributionChannel> distributionChannels = citTableDistributionChannelDAO.getActiveDistributionChannel();
		return distributionChannels;
	}
	
	@Override
	public String generateDistributionChannelNo(String type) {
		
		return citTableDistributionChannelDAO.generateDistributionChannelNo(type);
		
	}
	
	@Override
	public boolean distributionChannelExistByProperty(String property, String value,String mode, String oldValue) {
		return !citTableDistributionChannelDAO.getDistributionChannelByProperty(property, value,mode,oldValue).isEmpty();
	}
	
	@Override
	public String addNewDistributionChannel(CfgTblDistributionChannel cfgTblDistributionChannel) {
				
		return citTableDistributionChannelDAO.addNewDistributionChannel(cfgTblDistributionChannel);
	}

	@Override
	public String updateDistributionChannel(CfgTblDistributionChannel cfgTblDistributionChannel) {
		
		return citTableDistributionChannelDAO.updateDistributionChannel(cfgTblDistributionChannel);
	}

	@Override
	public String deleteDistributionChannel(List<String> distributionChannelsId) {
		// TODO Auto-generated method stub
		return citTableDistributionChannelDAO.deleteDistributionChannel(distributionChannelsId);
	}
	
	@Override
	public List<CfgTblDistributionChannel> searchDistributionChannel(CfgTblDistributionChannel distributionChannel) {
		// TODO Auto-generated method stub
		return citTableDistributionChannelDAO.searchDistributionChannel(distributionChannel);
	}

}
