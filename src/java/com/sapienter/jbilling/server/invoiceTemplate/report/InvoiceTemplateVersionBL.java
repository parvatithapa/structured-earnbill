package com.sapienter.jbilling.server.invoiceTemplate.report;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.invoice.InvoiceTemplateVersionDTO;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by prashant on 27/1/15.
 */
public class InvoiceTemplateVersionBL {

    private InvoiceTemplateVersionDTO templateVersionDTO;

    public InvoiceTemplateVersionBL(){

    }

    public InvoiceTemplateVersionBL(InvoiceTemplateVersionDTO templateVersionDTO){
        this.templateVersionDTO = templateVersionDTO;
    }

    public InvoiceTemplateVersionDTO getTemplateVersionDTO() {
        return templateVersionDTO;
    }

    public void setTemplateVersionDTO(InvoiceTemplateVersionDTO templateVersionDTO) {
        this.templateVersionDTO = templateVersionDTO;
    }

    public Boolean isLatestMajorVersion(){
        LinkedList<InvoiceTemplateVersionDTO> versions = sortDTOByVersionNumber(templateVersionDTO.getInvoiceTemplate().getInvoiceTemplateVersions());

        return (getTemplateVersionDTO().getVersionNumber().matches(getTemplateVersionDTO().getInvoiceTemplate().getId()+".\\d*")
                && versions.size() > 0)
                && versions.getLast().getVersionNumber().equalsIgnoreCase(getTemplateVersionDTO().getVersionNumber());
    }

    public Boolean isLatestMinorVersion(){
        LinkedList<InvoiceTemplateVersionDTO> versions = sortDTOByVersionNumber(findAllAdjacentVersions());

        return (versions.size() > 0) && versions.getLast().getVersionNumber().equalsIgnoreCase(getTemplateVersionDTO().getVersionNumber());
    }

    public List<InvoiceTemplateVersionDTO> findAllAdjacentVersions(String versionNumber){
        List<InvoiceTemplateVersionDTO> adjacentVersions = new ArrayList<InvoiceTemplateVersionDTO>();

        String versionPattern = getMinorVersionPattern(versionNumber);

        for (InvoiceTemplateVersionDTO versionDTO : templateVersionDTO.getInvoiceTemplate().getInvoiceTemplateVersions()){
            if(versionDTO.getVersionNumber().matches(versionPattern + "\\d*")){
                adjacentVersions.add(versionDTO);
            }
        }
        return adjacentVersions;
    }

    public List<InvoiceTemplateVersionDTO> findAllAdjacentVersions(){
        return findAllAdjacentVersions(templateVersionDTO.getVersionNumber());
    }

    public LinkedList<String> findAllMajorAdjacentVersions(){

        Set<String> majorAdjacentVersions = new HashSet<String>(0);
        Pattern pattern = Pattern.compile(templateVersionDTO.getInvoiceTemplate().getId()+".\\d*");

        for (InvoiceTemplateVersionDTO versionDTO : templateVersionDTO.getInvoiceTemplate().getInvoiceTemplateVersions()){
            Matcher matcher = pattern.matcher(versionDTO.getVersionNumber());
            if(matcher.find()){
                majorAdjacentVersions.add(matcher.group());
            }
        }
        return sortByVersionNumberDesc(majorAdjacentVersions);
    }

    /*
    *  creates an upgraded major version
    *  e.g. 2.2 is updated to 2.3 because 2.2 is the latest version.
    * */
    public InvoiceTemplateVersionDTO createNewMajorVersion(){
        InvoiceTemplateVersionDTO newDto = createNewVersionDTO();
        LinkedList<String> adjacentVersions = findAllMajorAdjacentVersions();

        if(adjacentVersions.size() > 0){
            String latestVersion = adjacentVersions.getFirst();
            String[] versionParts = latestVersion.split("\\.");
            newDto.setVersionNumber( getTemplateVersionDTO().getInvoiceTemplate().getId() + ("." + (Integer.valueOf(versionParts[1])+1) ));
        }else{
            newDto.setVersionNumber(getTemplateVersionDTO().getInvoiceTemplate().getId() +".1");
        }

        return newDto;
    }

    /*
    *  creates an upgraded minor version when current one is the latest minor version
    *  e.g. 2.2.3 is updated to 2.2.4 (because 2.2.3 is latest in 2.2.x )
    * */
    public InvoiceTemplateVersionDTO createUpgradedMinorVersion(){
        return createUpgradedMinorVersion(getTemplateVersionDTO().getVersionNumber());
    }

    public InvoiceTemplateVersionDTO createUpgradedMinorVersion(String versionNumber){
        InvoiceTemplateVersionDTO newDto = createNewVersionDTO();

        LinkedList<InvoiceTemplateVersionDTO> adjacentVersions = sortDTOByVersionNumber(findAllAdjacentVersions(versionNumber));

        String versionPattern = getMinorVersionPattern(versionNumber);
        newDto.setVersionNumber( versionPattern + (getRightmostVersionPart(adjacentVersions.getLast().getVersionNumber())+1) );

        return newDto;
    }

    /*
    *  creates a minor version when some older version is updated
    *  e.g. 2.2 is updated to 2.2.1 if 2.3 already present
    *  or 2.2 is updated to 2.2.3 (latest in 2.2.x series) if 2.2.1 and 2.2.2 are already present
    * */
    public InvoiceTemplateVersionDTO createMinorVersion(){
        InvoiceTemplateVersionDTO newDto;
        String newVersion = getTemplateVersionDTO().getVersionNumber() + "." +1;

        if(isVersionAlreadyPresent(newVersion)){
            newDto = createUpgradedMinorVersion(newVersion);
        }else{
            newDto = createNewVersionDTO();
            newDto.setVersionNumber(newVersion);
        }
        return newDto;
    }

    public InvoiceTemplateVersionDTO createNewVersion(){
        InvoiceTemplateVersionDTO newDto = null;
        if(isLatestMajorVersion()){
            newDto = createNewMajorVersion();
        }else if(isLatestMinorVersion()){
            newDto = createUpgradedMinorVersion();
        }else{
            newDto = createMinorVersion();
        }
        validateInvoiceTemplateVersionDTO(newDto);
        return newDto;
    }

    public InvoiceTemplateVersionDTO createNewVersionDTO(){
        InvoiceTemplateVersionDTO newDto = new InvoiceTemplateVersionDTO();
        newDto.setTemplateJson(getTemplateVersionDTO().getTemplateJson());
        newDto.setCreatedDatetime(TimezoneHelper.serverCurrentDate());
        newDto.setSize(getTemplateVersionDTO().getTemplateJson().length());
        return newDto;
    }

    public String getMinorVersionPattern(String versionNumber){
        versionNumber = versionNumber.trim();
        return versionNumber.lastIndexOf('.') >= 0 ? versionNumber.substring(0,versionNumber.lastIndexOf('.'))+"." : versionNumber;
    }

    public void validateInvoiceTemplateVersionDTO(InvoiceTemplateVersionDTO dto){
        if (isVersionAlreadyPresent(dto.getVersionNumber())){
            throw new SessionInternalError("Invoice template version already exist: " + dto.getVersionNumber(),
                    new String[] {"InvoiceTemplateVersionDTO,versionNumber,invoiceTemplateVersionDTO.versionNumber.duplicate.message,"+dto.getVersionNumber()});
        }
    }

    public boolean isVersionAlreadyPresent(String versionNumber){
        Collection<InvoiceTemplateVersionDTO> allVersions = getTemplateVersionDTO().getInvoiceTemplate().getInvoiceTemplateVersions();
        for (InvoiceTemplateVersionDTO version: allVersions){
            if (versionNumber.equals(version.getVersionNumber())){
                return true;
            }
        }
        return false;
    }

    public int getRightmostVersionPart(String version){
        String[] versionParts = version.split("\\.");
        return Integer.parseInt(versionParts[versionParts.length-1]);
    }

    public static LinkedList<InvoiceTemplateVersionDTO> sortDTOByVersionNumber(Collection<InvoiceTemplateVersionDTO> versions){
        LinkedList<InvoiceTemplateVersionDTO> sortedVersions = new LinkedList<InvoiceTemplateVersionDTO>(versions);
        sortedVersions.sort(new CompareInvoiceTemplateVersionByVerNumberAsc());
        return sortedVersions;
    }

    public static LinkedList<String> sortByVersionNumberDesc(Collection<String> versions){
        LinkedList<String> sortedVersions = new LinkedList<String>(versions);
        sortedVersions.sort(new VersionNumberComparatorDesc());
        return sortedVersions;
    }

    /**
    * returns 1 if o1 is latest, -1 if o2 is latest and 0 if both are same.
    * */
    public static int compareVersions(InvoiceTemplateVersionDTO o1, InvoiceTemplateVersionDTO o2){
        return compareVersions(o1.getVersionNumber(), o2.getVersionNumber());
    }

    public static int compareVersions(String version1, String version2){
        String[] versions1 = version1.split("\\.");
        String[] versions2 = version2.split("\\.");

        int length = Math.min(versions1.length,versions2.length);
        int comparison = 0;
        for(int i = 0; i < length; i++){
            comparison = Integer.valueOf(versions1[i]).compareTo(Integer.valueOf(versions2[i]));
            if(comparison != 0) break;
        }

            /*
            * 'comparison' will come 0 if versions 1.1.1 and 1.1 are compared
            * so we make last attempt using length for comparison as 1.1.1 will be the latest one than 1.1
            * */
        return (comparison == 0) ? Integer.valueOf(versions1.length).compareTo(versions2.length) : comparison;
    }

    public static class CompareInvoiceTemplateVersionByVerNumberAsc implements Comparator<InvoiceTemplateVersionDTO> {

        @Override
        public int compare(InvoiceTemplateVersionDTO o1, InvoiceTemplateVersionDTO o2) {
            return compareVersions(o1, o2);
        }
    }

    public static class CompareInvoiceTemplateVersionByVerNumberDesc implements Comparator<InvoiceTemplateVersionDTO> {

        @Override
        public int compare(InvoiceTemplateVersionDTO o1, InvoiceTemplateVersionDTO o2) {
            return compareVersions(o1, o2) * -1;
        }
    }

    public static class VersionNumberComparatorDesc implements Comparator<String> {

        @Override
        public int compare(String version1, String version2) {
            return compareVersions(version1, version2) * -1;
        }
    }

    public static void updateUseForInvoice(InvoiceTemplateVersionDTO versionDTO, Boolean useForInvoice){
        LinkedList<InvoiceTemplateVersionDTO> versionDTOs = sortDTOByVersionNumber(versionDTO.getInvoiceTemplate().getInvoiceTemplateVersions());
        InvoiceTemplateVersionDTO versionSetForInvoice = getVersionUsedForInvoice(versionDTOs);

        if (versionSetForInvoice == null){
            versionDTO.setUseForInvoice(useForInvoice);
        }else{
            versionSetForInvoice.setUseForInvoice(Boolean.FALSE);
            versionDTO.setUseForInvoice(useForInvoice);
        }
    }

    public static InvoiceTemplateVersionDTO getVersionUsedForInvoice(LinkedList<InvoiceTemplateVersionDTO> versionDTOs){
        InvoiceTemplateVersionDTO retVal=null;

        for (InvoiceTemplateVersionDTO dto : versionDTOs){
            if (dto.getUseForInvoice()){
                retVal = dto;
                break;
            }
        }
        return retVal;
    }

    /**
     * returns the version which has useForInvoice set to true,
     * if no version is set for invoice then the latest version is returned
     * */
    public static InvoiceTemplateVersionDTO getVersionForInvoice(LinkedList<InvoiceTemplateVersionDTO> versionDTOs){
        InvoiceTemplateVersionDTO retVal = getVersionUsedForInvoice(versionDTOs);

        if (retVal == null){
            retVal = versionDTOs.size() > 0 ? versionDTOs.getLast() : null;
        }

        return retVal;
    }

    public static InvoiceTemplateVersionDTO getVersionForInvoice(Set<InvoiceTemplateVersionDTO> versionDTOs){
        return getVersionForInvoice(sortDTOByVersionNumber(versionDTOs));
    }

    public static Boolean validateVersionForDelete(InvoiceTemplateVersionDTO versionDTO){

        if(versionDTO.getInvoiceTemplate().getInvoiceTemplateVersions().size() == 1){
            throw new SessionInternalError("At least one invoice template version should be present",
                    new String[]{"invoice.template.version.in.use.message,"+versionDTO.getId()});
        }
        return Boolean.TRUE;
    }
}
