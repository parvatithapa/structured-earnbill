package com.sapienter.jbilling.server.usageratingscheme.service;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.exception.QuantityRatingException;
import com.sapienter.jbilling.server.usageRatingScheme.domain.IUsageRatingSchemeModel;
import com.sapienter.jbilling.server.usageRatingScheme.scheme.IUsageRatingScheme;
import com.sapienter.jbilling.server.usageRatingScheme.util.AttributeUtils;
import com.sapienter.jbilling.server.usageratingscheme.DynamicAttributeLineWS;
import com.sapienter.jbilling.server.usageratingscheme.UsageRatingSchemeWS;
import com.sapienter.jbilling.server.usageratingscheme.domain.UsageRatingSchemeModel;
import com.sapienter.jbilling.server.usageratingscheme.domain.UsageRatingSchemeType;
import com.sapienter.jbilling.server.usageratingscheme.domain.entity.DynamicAttributeLineDTO;
import com.sapienter.jbilling.server.usageratingscheme.domain.entity.UsageRatingSchemeDTO;
import com.sapienter.jbilling.server.usageratingscheme.domain.entity.UsageRatingSchemeTypeDTO;
import com.sapienter.jbilling.server.usageratingscheme.domain.repository.UsageRatingSchemeDAS;
import com.sapienter.jbilling.server.usageratingscheme.domain.repository.UsageRatingSchemeTypeDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import org.apache.http.HttpStatus;
import org.hibernate.HibernateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.util.CollectionUtils;

import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.stream.Collectors;


public class UsageRatingSchemeBL {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String USAGE_RATING_SCHEME_VALIDATION_FAILED="Usage Rating Scheme validation failed.";
    private static final String DATA_ACCESS_ISSUE="Data access issue: ";
    private UsageRatingSchemeDAS das = null;
    private UsageRatingSchemeDTO dto = null;
    private UsageRatingSchemeTypeDAS typeDAS = null;

    public UsageRatingSchemeBL() {
        this.das = new UsageRatingSchemeDAS();
        this.typeDAS = new UsageRatingSchemeTypeDAS();
    }

    public UsageRatingSchemeBL(UsageRatingSchemeDTO dto) {
        this();
        this.dto = dto;
    }

    public UsageRatingSchemeBL(Integer id) {
        this();
        this.dto = this.das.find(id);
    }

    public static UsageRatingSchemeDTO getDTO(UsageRatingSchemeWS ws) {
        UsageRatingSchemeDTO dto = new UsageRatingSchemeDTO();
        dto.setId(ws.getId());
        dto.setEntity(new CompanyDTO(ws.getEntityId()));
        dto.setRatingSchemeCode(ws.getRatingSchemeCode().trim());

        UsageRatingSchemeBL bl = new UsageRatingSchemeBL();
        dto.setRatingSchemeType(bl.findRatingSchemeTypeByName(ws.getRatingSchemeType()));

        dto.setFixedAttributes(Optional
                .ofNullable(ws.getFixedAttributes())
                .orElse(new HashMap<>())
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        e -> e.getKey(),
                        e -> e.getValue().trim())
                ));

        dto.setUsesDynamicAttributes(ws.isUsesDynamicAttributes());
        dto.setDynamicAttributeName(ws.getDynamicAttributeName());

        SortedSet<DynamicAttributeLineDTO> dynamicAttributeLineDTOs = Optional
                .ofNullable(ws.getDynamicAttributes())
                .orElse(new TreeSet<>())
                .stream()
                .map(attr -> {
                    DynamicAttributeLineDTO attrDTO = new DynamicAttributeLineDTO();
                    attrDTO.setId(attr.getId());
                    attrDTO.setSequence(attr.getSequence());
                    attrDTO.setAttributes(attr.getAttributes().entrySet()
                            .stream()
                            .collect(Collectors.toMap(
                                    e -> e.getKey(),
                                    e -> e.getValue().trim())
                            ));
                    attrDTO.setRatingScheme(dto);

                    return attrDTO;
                })
                .collect(Collectors.toCollection(() -> new TreeSet<>()));

        dto.setDynamicAttributes(dynamicAttributeLineDTOs);

        logger.info("WSToDTO DTO object: {}", dto);
        return dto;
    }

    public static UsageRatingSchemeWS getWS(UsageRatingSchemeDTO dto) {
        UsageRatingSchemeWS ws = new UsageRatingSchemeWS();
        ws.setId(dto.getId());
        ws.setEntityId(dto.getEntity().getId());
        ws.setRatingSchemeCode(dto.getRatingSchemeCode());
        ws.setRatingSchemeType(dto.getRatingSchemeType().getName());
        ws.setFixedAttributes(dto.getFixedAttributes());
        ws.setUsesDynamicAttributes(dto.usesDynamicAttributes());
        ws.setDynamicAttributeName(dto.getDynamicAttributeName());

        SortedSet<DynamicAttributeLineWS> dynamicAttributeLines = Optional
                .ofNullable(dto.getDynamicAttributes())
                .orElse(new TreeSet<>())
                .stream()
                .map(attrDTO -> {
                    DynamicAttributeLineWS attr = new DynamicAttributeLineWS();
                    attr.setId(attrDTO.getId());
                    attr.setSequence(attrDTO.getSequence());

                    Map<String, String> result = new HashMap<>();
                    attrDTO.getAttributes().forEach((k, v) ->
                            result.put(k, v)
                    );
                    attr.setAttributes(result);

                    return attr;
                })
                .collect(Collectors.toCollection(() -> new TreeSet<>()));

        ws.setDynamicAttributes(dynamicAttributeLines);

        logger.info("DTOToWS WS object: {}", ws);
        return ws;
    }

    public static UsageRatingSchemeWS getWsRatingSchemeForDate(SortedMap<Date, UsageRatingSchemeWS> usageRatingSchemes,
                                                               Date date) {

        if (usageRatingSchemes == null) {
            logger.info("No rating scheme available");
            return null;
        }

        if (date == null) {
            return usageRatingSchemes.get(usageRatingSchemes.firstKey());
        }

        // list of prices in ordered by start date, earliest first
        // return the model with the closest start date
        Date forDate = null;
        for (Date start : usageRatingSchemes.keySet()) {
            if (start != null && start.after(date)) {
                break;
            }

            forDate = start;
        }

        forDate = Optional.ofNullable(forDate).orElse(usageRatingSchemes.firstKey());

        logger.info("Rating scheme for date: {}", forDate);
        return usageRatingSchemes.get(forDate);
    }

    public static IUsageRatingSchemeModel convertDTOToModel(UsageRatingSchemeDTO dto) {

        if (dto == null) {
            logger.info("Null dto, returning");
            return null;
        }

        List<Map<String, String>> dynamicAttributes = Optional
                .ofNullable(dto.getDynamicAttributes())
                .orElse(new TreeSet<>())
                .stream()
                .map(line -> {
                    Map<String, String> result = new HashMap<>();
                    line.getAttributes().forEach((k, v) ->
                            result.put(k, v)
                    );
                    return result;
                })
                .collect(Collectors.toList());

        UsageRatingSchemeModel model = new UsageRatingSchemeModel(dto.getRatingSchemeCode(),
                convertDtoToUsageRatingSchemeType(dto.getRatingSchemeType()).getUsageRatingScheme(),
                dto.getFixedAttributes(), dynamicAttributes);

        logger.info("UsageRatingSchemeModel obj: {}", model);
        return model;
    }

    public static IUsageRatingSchemeModel convertWSToModel(UsageRatingSchemeWS ws) {

        if (ws == null) {
            logger.info("Null dto, returning");
            return null;
        }

        List<Map<String, String>> dynamicAttributes =
                Optional.ofNullable(ws.getDynamicAttributes())
                        .orElse(new TreeSet<>())
                        .stream()
                        .map(line -> line.getAttributes())
                        .collect(Collectors.toList());

        UsageRatingSchemeBL bl = new UsageRatingSchemeBL();
        UsageRatingSchemeModel model = new UsageRatingSchemeModel(ws.getRatingSchemeCode(),
                bl.findUsageRatingSchemeInstanceByName(ws.getRatingSchemeType()),
                ws.getFixedAttributes(), dynamicAttributes);

        logger.info("UsageRatingSchemeModel obj: {}", model);
        return model;
    }

    public static UsageRatingSchemeType convertDtoToUsageRatingSchemeType(UsageRatingSchemeTypeDTO dto) {

        UsageRatingSchemeType type = dto == null ?
                null :
                new UsageRatingSchemeType(dto.getName(),
                        UsageRatingSchemeFactory.getInstance(dto.getName()));

        logger.info("Type: {}", type);
        return type;
    }

    public UsageRatingSchemeWS getWS() {
        return getWS(dto);
    }

    private boolean isFieldEmpty(String val) {
        return val == null || val.trim().isEmpty();
    }

    private boolean isRatingSchemeCodeUnique(Integer entityId, String code) {
        return this.das.getCountByRatingSchemeCode(entityId, code.trim()) == 0L;
    }

    public void validateUsageRatingScheme(UsageRatingSchemeWS ws) {
        List<String> errors = new ArrayList<>();

        if (isFieldEmpty(ws.getRatingSchemeType()))
            errors.add("Usage Rating Scheme type is mandatory");

        if (isFieldEmpty(ws.getRatingSchemeCode()))
            errors.add("Usage Rating Scheme code is mandatory");

        if (!CollectionUtils.isEmpty(errors)) {
            throw new SessionInternalError(USAGE_RATING_SCHEME_VALIDATION_FAILED,
                    errors.toArray(new String[errors.size()]));
        }

        if (ws.getId() == null &&
                !isRatingSchemeCodeUnique(ws.getEntityId(), ws.getRatingSchemeCode())) {

            throw new SessionInternalError(USAGE_RATING_SCHEME_VALIDATION_FAILED,
                    new String[] { "Rating Scheme Code already exists" });
        }

        IUsageRatingScheme ratingScheme = findUsageRatingSchemeInstanceByName(
                ws.getRatingSchemeType());

        Map<String, String> fixedAttributes = ws.getFixedAttributes();
        if (fixedAttributes != null) {
            errors.addAll(AttributeUtils.validateAttributes(fixedAttributes,
                    ws.getRatingSchemeType(), ratingScheme.getFixedAttributes(), null));
        }

        if (ws.isUsesDynamicAttributes()) {
            if (!CollectionUtils.isEmpty(ws.getDynamicAttributes())) {
                for (DynamicAttributeLineWS line : ws.getDynamicAttributes()) {
                    Map<String, String> dynamicAttributes = line.getAttributes();
                    errors.addAll(AttributeUtils.validateAttributes(dynamicAttributes,
                            ws.getRatingSchemeType(), ratingScheme.getDynamicAttributes(),
                            line.getSequence() + 1));
                }
            } else {
                errors.add("Atleast one row of attributes is required");
            }
        }

        if (!CollectionUtils.isEmpty(errors)) {
            throw new SessionInternalError(USAGE_RATING_SCHEME_VALIDATION_FAILED,
                    errors.toArray(new String[errors.size()]));
        }

        try {
            ratingScheme.validate(convertWSToModel(ws));

        } catch (QuantityRatingException e) {
            errors.add("Not a valid definition of rating scheme " + ws.getRatingSchemeType()
                    + ". Reason -\n" + e.getErrorMessages()[0]);
        }

        if (!CollectionUtils.isEmpty(errors)) {
            throw new SessionInternalError(USAGE_RATING_SCHEME_VALIDATION_FAILED,
                    errors.toArray(new String[errors.size()]));
        }
    }

    public UsageRatingSchemeDTO findEffectiveRatingSchemeByItem(Integer itemId, Date eventDate) {
        UsageRatingSchemeDTO usageRatingScheme;
        try {
            usageRatingScheme = this.das.findTopByItemIdAndEventDateLesserThan(itemId, eventDate);
        } catch (HibernateException he) {
            logger.error("Exception in findEffectiveRatingSchemeByItem ", he);
            throw new SessionInternalError(DATA_ACCESS_ISSUE + he.getMessage(),
                    HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }

        return usageRatingScheme;
    }

    public NavigableMap<Date, IUsageRatingSchemeModel> findAllByItem(Integer itemId) {

        NavigableMap<Date, IUsageRatingSchemeModel> result = new TreeMap<>();
        try {
            Map<Date, UsageRatingSchemeDTO> dtos = this.das.findAllByItem(itemId);
            for (Map.Entry<Date, UsageRatingSchemeDTO> e : dtos.entrySet()) {
                result.put(e.getKey(), convertDTOToModel(e.getValue()));
            }

        } catch (HibernateException he) {
            logger.error("Exception in findAllByItem ", he);
            throw new SessionInternalError(DATA_ACCESS_ISSUE + he.getMessage(),
                    HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
        return result;
    }

    public UsageRatingSchemeDTO create(UsageRatingSchemeDTO dto) {
        try {
            dto = das.save(dto);

        } catch(HibernateException he) {
            logger.error("Exception in create ", he);
            throw new SessionInternalError("Data access issue while creating rating scheme: " + he.getMessage(),
                    HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }

        logger.info("DTO created with id: {}", dto.getId());
        return dto;
    }

    public boolean delete() {
        try {
            List<Integer> productsIds = das.findProductsAssociatedWithRatingScheme(dto.getId());
            if (!CollectionUtils.isEmpty(productsIds)) {
                throw new SessionInternalError("Cannot delete rating scheme, " +
                        "following products are using it: " + productsIds, HttpStatus.SC_CONFLICT);
            }
        } catch (HibernateException he) {
            logger.error("Exception in delete ", he);
            throw new SessionInternalError(DATA_ACCESS_ISSUE + he.getMessage(),
                    HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }

        try {
            das.delete(dto);

        } catch(DataAccessException dae) {
            logger.error("Exception in delete ", dae);
            throw new SessionInternalError("Data access issue while deleting rating scheme: " + dae.getMessage(),
                    HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
        return true;
    }

    public UsageRatingSchemeDTO getDTO() {
        return dto;
    }

    public IUsageRatingScheme findUsageRatingSchemeInstanceByName(String name) {
        UsageRatingSchemeType type = convertDtoToUsageRatingSchemeType(findRatingSchemeTypeByName(name));

        logger.info("Type: {}", type);
        return type.getUsageRatingScheme();
    }

    public Long countAllRatingSchemes(Integer entityId) {
        try {
            return das.countAll(entityId);

        } catch(HibernateException he) {
            logger.error("Exception in countAllRatingSchemes ", he);
            throw new SessionInternalError(DATA_ACCESS_ISSUE + he.getMessage(),
                    HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
    }

    public List<UsageRatingSchemeDTO> findAll(Integer entityId, Integer max, Integer offset) {
        try {
            return das.findAll(entityId, max, offset);

        } catch(HibernateException he) {
            logger.error("Exception in findAll ", he);
            throw new SessionInternalError(DATA_ACCESS_ISSUE + he.getMessage(),
                    HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
    }

    public List<UsageRatingSchemeTypeDTO> findAllRatingSchemeTypes() {
        List<UsageRatingSchemeTypeDTO> allActive;
        try {
            allActive = typeDAS.findAllActive();

        } catch(HibernateException he) {
            logger.error("Exception in findAllRatingSchemeTypes ", he);
            throw new SessionInternalError(DATA_ACCESS_ISSUE + he.getMessage(),
                    HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
        logger.info("All active count {}", allActive.size());
        return allActive;
    }

    public List<UsageRatingSchemeType> findAllRatingSchemeTypeValues() {
        List<UsageRatingSchemeTypeDTO> ratingSchemeTypes = findAllRatingSchemeTypes();

        return ratingSchemeTypes.stream()
                .map(type ->
                        new UsageRatingSchemeType(
                                type.getName(),
                                UsageRatingSchemeFactory.getInstance(type.getName()))
                )
                .collect(Collectors.toList());
    }

    public UsageRatingSchemeTypeDTO findRatingSchemeTypeByName(String name) {
        String resolvedName = Optional.ofNullable(name)
                .filter(s -> !s.isEmpty())
                .orElseThrow(() ->
                        new QuantityRatingException("Cannot query rating scheme with null/empty name")
                );

        try {
            return Optional.ofNullable(typeDAS.findOneByName(name))
                    .orElseThrow(() ->
                            new QuantityRatingException("Rating scheme Not found with name " + resolvedName)
                    );
        } catch(HibernateException he) {
            logger.error("Exception in findRatingSchemeTypeByName ", he);
            throw new SessionInternalError(DATA_ACCESS_ISSUE + he.getMessage(),
                    HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
