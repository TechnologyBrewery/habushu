from ...generated.step.abstract_pipeline_step import AbstractPipelineStep
from abc import abstractmethod

class IngestBase(AbstractPipelineStep):
    def __init__(self, data_action_type, descriptive_label):
        super().__init__(data_action_type, descriptive_label)

    def execute_step(self) -> None: # pylint: disable=invalid-name
        # pylint: disable-next=assignment-from-none
        event_data = self.create_base_lineage_event_data()

    @abstractmethod
    def execute_step_impl(self) -> None:
        pass
