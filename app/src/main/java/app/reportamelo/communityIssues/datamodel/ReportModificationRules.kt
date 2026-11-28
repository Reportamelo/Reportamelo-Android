package app.reportamelo.communityIssues.datamodel

import app.reportamelo.commons.IssueStatus
import app.reportamelo.commons.ViewOptions

/** Estatus en los que el ciudadano puede modificar su reporte. Espejo de iOS MyReportsSubView. */
val EDITABLE_REPORT_STATUSES: Set<IssueStatus> = setOf(IssueStatus.REPORTED, IssueStatus.RECTIFICATION)

/** Un reporte se modifica solo desde la vista listAndModify y con un estatus editable. */
fun canModifyReport(mode: ViewOptions, status: IssueStatus): Boolean =
    mode == ViewOptions.LIST_AND_MODIFY && status in EDITABLE_REPORT_STATUSES
